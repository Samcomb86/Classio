package com.example.notifications

import com.example.model.Lecture
import com.example.model.TeacherIndex
import com.example.model.TimetableData
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object NotificationScheduler {

    private val karachiTimeZone: TimeZone = TimeZone.getTimeZone("Asia/Karachi")

    fun buildSchedule(
        data: TimetableData?,
        classId: String?,
        settings: NotificationSettings,
        nowCalendar: Calendar,
        horizonDays: Int = 7,
        teacherIndex: TeacherIndex? = null
    ): List<ScheduledNotification> {
        if (!settings.enabled || classId.isNullOrBlank() || data == null) {
            return emptyList()
        }

        val result = mutableListOf<ScheduledNotification>()

        val baseCal = Calendar.getInstance(karachiTimeZone).apply {
            timeInMillis = nowCalendar.timeInMillis
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = karachiTimeZone
        }
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = karachiTimeZone
        }
        val dayNameFormat = SimpleDateFormat("EEEE", Locale.US).apply {
            timeZone = karachiTimeZone
        }

        for (dayOffset in 0 until horizonDays) {
            val dayCal = (baseCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }

            val dateStr = dateFormat.format(dayCal.time)
            val dayName = dayNameFormat.format(dayCal.time)

            val lectures = getLecturesForClassAndDay(data, classId, dayName, teacherIndex)
            if (lectures.isEmpty()) continue

            // 1. Lecture Reminders
            for (lecture in lectures) {
                val (startHour, startMinute) = parseTime(lecture.start) ?: continue
                val (endHour, endMinute) = parseTime(lecture.end) ?: Pair(startHour + 1, startMinute)

                val lectureStartCal = (dayCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, startHour)
                    set(Calendar.MINUTE, startMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val fireAtCal = (lectureStartCal.clone() as Calendar).apply {
                    add(Calendar.MINUTE, -settings.leadMinutes)
                }

                val fireAtMillis = fireAtCal.timeInMillis
                if (fireAtMillis <= baseCal.timeInMillis) {
                    continue
                }

                val notificationId = generateDeterministicId(dateStr, lecture.start, classId)
                val lectureKey = "$dateStr|$classId|${lecture.start}"

                val title = "Next lecture in ${settings.leadMinutes} min"
                val body = formatLectureNotificationBody(lecture, startHour, startMinute, endHour, endMinute)

                result.add(
                    ScheduledNotification(
                        id = notificationId,
                        fireAtMillis = fireAtMillis,
                        fireAtIso = isoFormat.format(Date(fireAtMillis)),
                        title = title,
                        body = body,
                        lectureKey = lectureKey,
                        type = NotificationType.LECTURE_REMINDER
                    )
                )
            }

            // 2. Morning Summary Notification (if enabled)
            if (settings.morningSummaryEnabled) {
                val (summaryHour, summaryMinute) = parseTime(settings.morningSummaryTime) ?: Pair(7, 0)
                val summaryCal = (dayCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, summaryHour)
                    set(Calendar.MINUTE, summaryMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val summaryMillis = summaryCal.timeInMillis
                if (summaryMillis > baseCal.timeInMillis) {
                    val firstLecture = lectures.minByOrNull { parseMinutes(it.start) }
                    if (firstLecture != null) {
                        val roomFormatted = formatRoomText(firstLecture.room)
                        val summaryId = generateDeterministicId(dateStr, "morning_summary", classId)
                        val summaryKey = "$dateStr|$classId|morning_summary"
                        val summaryTitle = "Today's Schedule"
                        val summaryBody = "Today: ${lectures.size} lectures, first at ${firstLecture.start} in $roomFormatted"

                        result.add(
                            ScheduledNotification(
                                id = summaryId,
                                fireAtMillis = summaryMillis,
                                fireAtIso = isoFormat.format(Date(summaryMillis)),
                                title = summaryTitle,
                                body = summaryBody,
                                lectureKey = summaryKey,
                                type = NotificationType.MORNING_SUMMARY
                            )
                        )
                    }
                }
            }
        }

        return result.sortedBy { it.fireAtMillis }
    }

    private fun getLecturesForClassAndDay(
        data: TimetableData,
        classId: String,
        dayName: String,
        teacherIndex: TeacherIndex?
    ): List<Lecture> {
        if (classId.startsWith("teacher:")) {
            val teacherKey = classId.removePrefix("teacher:")
            val teacherLecs = teacherIndex?.byTeacher?.get(teacherKey)?.get(dayName) ?: emptyList()
            val teacherName = teacherIndex?.teachers?.find { it.key == teacherKey }?.name ?: teacherKey
            return teacherLecs.map { tLec ->
                Lecture(
                    start = tLec.start,
                    end = tLec.end,
                    subject = tLec.subject,
                    code = tLec.code,
                    type = tLec.type,
                    teacher = teacherName,
                    teacherKey = teacherKey,
                    room = tLec.room,
                    raw = tLec.classLabels.joinToString(", ")
                )
            }
        }

        val classMap = data.timetable[classId] ?: return emptyList()
        val key = classMap.keys.find { it.equals(dayName, ignoreCase = true) } ?: return emptyList()
        return classMap[key] ?: emptyList()
    }

    fun formatLectureNotificationBody(
        lecture: Lecture,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ): String {
        val typeSuffix = if (!lecture.type.isNullOrBlank()) " (${lecture.type})" else ""
        val subjectWithType = "${lecture.subject}$typeSuffix"

        val roomFormatted = formatRoomText(lecture.room)

        val teacherFormatted = if (!lecture.teacher.isNullOrBlank()) {
            lecture.teacher
        } else {
            "Teacher not announced"
        }

        val timeRangeFormatted = String.format(
            Locale.US,
            "%02d:%02d to %02d:%02d",
            startHour,
            startMinute,
            endHour,
            endMinute
        )

        return "$subjectWithType | Room $roomFormatted | $teacherFormatted | $timeRangeFormatted"
    }

    fun formatRoomText(room: String?): String {
        return when {
            room.isNullOrBlank() -> "Room not assigned yet"
            room.equals("CL", ignoreCase = true) -> "Computer Lab (CL)"
            else -> room
        }
    }

    fun generateDeterministicId(dateStr: String, startOrTag: String, classId: String): Int {
        val raw = "$dateStr|$classId|$startOrTag"
        return (raw.hashCode() and 0x7FFFFFFF)
    }

    private fun parseTime(time: String): Pair<Int, Int>? {
        return try {
            val parts = time.trim().split(":")
            Pair(parts[0].toInt(), parts[1].toInt())
        } catch (_: Exception) {
            null
        }
    }

    private fun parseMinutes(time: String): Int {
        val parsed = parseTime(time) ?: return 0
        return parsed.first * 60 + parsed.second
    }
}
