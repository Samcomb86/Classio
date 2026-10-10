package com.example.parser

import com.example.model.*
import java.util.Locale

object TeacherNormalizer {

    private val TITLE_REGEX = Regex(
        """^(Mr|Mrs|Ms|Miss|Dr|Prof|Engr|Sir|Madam)(\.?)(\s*)""",
        RegexOption.IGNORE_CASE
    )

    fun normalizeTeacher(raw: String?): String? {
        if (raw == null) return null
        var text = raw.trim()
        if (text.isEmpty()) return null

        // 1. Remove brackets and parentheses
        text = text.replace("(", " ")
            .replace(")", " ")
            .replace("[", " ")
            .replace("]", " ")
            .replace("\\s+".toRegex(), " ")
            .trim()

        // 3. Treat "?" or empty as null
        if (text.isEmpty() || text == "?" || text.equals("Teacher not announced", ignoreCase = true)) {
            return null
        }

        // 2. Make sure there is exactly one space after a title with a dot (or add dot if standard)
        val match = TITLE_REGEX.find(text)
        if (match != null) {
            val titleWord = match.groupValues[1]
            val hasDot = match.groupValues[2] == "." || titleWord.length <= 4 // Standardize title
            val rest = text.substring(match.range.last + 1).trim()
            if (rest.isEmpty()) return null
            val standardizedTitle = when (titleWord.lowercase(Locale.ROOT)) {
                "mr" -> "Mr."
                "mrs" -> "Mrs."
                "ms" -> "Ms."
                "miss" -> "Miss"
                "dr" -> "Dr."
                "prof" -> "Prof."
                "engr" -> "Engr."
                "sir" -> "Sir"
                "madam" -> "Madam"
                else -> if (hasDot) "$titleWord." else titleWord
            }
            text = "$standardizedTitle $rest"
        }

        return text.trim()
    }

    fun getTeacherKey(normalized: String?): String? {
        if (normalized == null) return null
        return normalized.lowercase(Locale.ROOT)
            .replace(".", "")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    fun getSortName(name: String): String {
        val match = TITLE_REGEX.find(name.trim())
        return if (match != null) {
            name.substring(match.range.last + 1).trim()
        } else {
            name.trim()
        }
    }

    /**
     * Build TeacherIndex from TimetableData.
     * Merges multi-section lectures at same time/room/subject into single card with multiple classLabels.
     * Detects overlapping lectures on same day for clash badge.
     */
    fun buildTeacherIndex(
        data: TimetableData,
        departmentName: String = "Faculty of Computing"
    ): TeacherIndex {
        // Collect lectures by teacherKey -> day -> raw lecture entries
        data class RawTeacherEntry(
            val lecture: Lecture,
            val classLabel: String,
            val day: String
        )

        val entriesByTeacher = mutableMapOf<String, MutableList<RawTeacherEntry>>()
        val displayNames = mutableMapOf<String, String>()

        for (classInfo in data.classes) {
            val dayMap = data.timetable[classInfo.id] ?: continue
            for ((day, lectures) in dayMap) {
                for (lec in lectures) {
                    val key = lec.teacherKey ?: getTeacherKey(normalizeTeacher(lec.teacher))
                    if (key.isNullOrBlank()) continue

                    val displayName = lec.teacher ?: normalizeTeacher(lec.teacher) ?: key
                    displayNames.putIfAbsent(key, displayName)

                    val list = entriesByTeacher.getOrPut(key) { mutableListOf() }
                    list.add(RawTeacherEntry(lec, classInfo.label, day))
                }
            }
        }

        val byTeacherMap = mutableMapOf<String, MutableMap<String, List<TeacherLecture>>>()
        val teacherInfoList = mutableListOf<TeacherInfo>()

        for ((teacherKey, entries) in entriesByTeacher) {
            val teacherName = displayNames[teacherKey] ?: teacherKey
            val daySchedules = mutableMapOf<String, List<TeacherLecture>>()

            // Group by day
            val entriesByDay = entries.groupBy { it.day }
            var totalLectureCount = 0

            for (day in data.days) {
                val dayEntries = entriesByDay[day] ?: emptyList()
                if (dayEntries.isEmpty()) {
                    daySchedules[day] = emptyList()
                    continue
                }

                // 1. Group / Merge same subject, room, start, end into single lecture with multiple classLabels
                // (e.g. SS1 and SS2 taught together)
                val mergedMap = mutableMapOf<String, TeacherLecture>()
                for (entry in dayEntries) {
                    val lec = entry.lecture
                    val mergeKey = "${lec.start}_${lec.end}_${lec.subject.lowercase(Locale.ROOT)}_${lec.room ?: ""}"
                    val existing = mergedMap[mergeKey]
                    if (existing != null) {
                        val updatedLabels = (existing.classLabels + entry.classLabel).distinct()
                        mergedMap[mergeKey] = existing.copy(classLabels = updatedLabels)
                    } else {
                        mergedMap[mergeKey] = TeacherLecture(
                            start = lec.start,
                            end = lec.end,
                            subject = lec.subject,
                            code = lec.code,
                            type = lec.type,
                            room = lec.room,
                            classLabels = listOf(entry.classLabel),
                            clash = false
                        )
                    }
                }

                // 2. Sort by start time
                val dayList = mergedMap.values.sortedBy { parseMinutes(it.start) }

                // 3. Detect clashes (overlapping times on same day)
                val checkedClashes = dayList.mapIndexed { idx, item ->
                    val itemStart = parseMinutes(item.start)
                    val itemEnd = parseMinutes(item.end)
                    val hasClash = dayList.indices.any { otherIdx ->
                        if (idx == otherIdx) false
                        else {
                            val other = dayList[otherIdx]
                            val oStart = parseMinutes(other.start)
                            val oEnd = parseMinutes(other.end)
                            // Overlaps if not completely before and not completely after
                            itemStart < oEnd && itemEnd > oStart
                        }
                    }
                    if (hasClash) item.copy(clash = true) else item
                }

                daySchedules[day] = checkedClashes
                totalLectureCount += checkedClashes.size
            }

            byTeacherMap[teacherKey] = daySchedules
            teacherInfoList.add(
                TeacherInfo(
                    key = teacherKey,
                    name = teacherName,
                    department = departmentName,
                    lectureCount = totalLectureCount
                )
            )
        }

        // Sort teachers alphabetically by name without title
        val sortedTeachers = teacherInfoList.sortedBy { getSortName(it.name).lowercase(Locale.ROOT) }

        return TeacherIndex(
            teachers = sortedTeachers,
            byTeacher = byTeacherMap
        )
    }

    private fun parseMinutes(timeStr: String): Int {
        val parts = timeStr.split(":")
        if (parts.size != 2) return 0
        return (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
    }
}
