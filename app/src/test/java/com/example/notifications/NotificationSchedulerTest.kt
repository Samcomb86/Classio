package com.example.notifications

import com.example.model.ClassInfo
import com.example.model.Lecture
import com.example.model.MetaData
import com.example.model.TimetableData
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class NotificationSchedulerTest {

    private val karachiTz = TimeZone.getTimeZone("Asia/Karachi")

    private fun createSampleData(): TimetableData {
        val lecturesMonday = listOf(
            Lecture(start = "08:00", end = "10:00", subject = "Programming Fundamentals", code = "CS-101", type = "Theory", teacher = "Mr. Ali", room = "R110"),
            Lecture(start = "10:00", end = "12:00", subject = "Quantitative Reasoning-I", code = "QR-101", type = "Theory", teacher = "Dr. Khan", room = "R112")
        )

        val timetable = mapOf(
            "BSCS-1-R" to mapOf("Monday" to lecturesMonday),
            "BSIT-1-R" to mapOf("Monday" to listOf(
                Lecture(start = "09:00", end = "11:00", subject = "Intro to IT", code = "IT-101", type = "Theory", teacher = "Ms. Sara", room = "R201")
            ))
        )

        return TimetableData(
            meta = MetaData(version = "V1.6"),
            timeSlots = emptyList(),
            days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday"),
            classes = listOf(
                ClassInfo(id = "BSCS-1-R", label = "BSCS 1ST R", programCode = "BSCS", program = "BS Computer Science", semester = 1, shift = "regular"),
                ClassInfo(id = "BSIT-1-R", label = "BSIT 1ST R", programCode = "BSIT", program = "BS Information Technology", semester = 1, shift = "regular")
            ),
            timetable = timetable
        )
    }

    private fun getMondayAt(hour: Int, minute: Int): Calendar {
        return Calendar.getInstance(karachiTz).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 5) // Monday
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    @Test
    fun `BSCS 1ST R Monday leadMinutes 10 schedules 07-50 and 09-50`() {
        val data = createSampleData()
        val now = getMondayAt(6, 0) // Monday 06:00 AM
        val settings = NotificationSettings(enabled = true, leadMinutes = 10)

        val list = NotificationScheduler.buildSchedule(data, "BSCS-1-R", settings, now, horizonDays = 1)

        val mondayReminders = list.filter { it.type == NotificationType.LECTURE_REMINDER }
        assertEquals(2, mondayReminders.size)

        val firstNotif = mondayReminders[0]
        val secondNotif = mondayReminders[1]

        assertTrue(firstNotif.fireAtIso.contains("07:50:00"))
        assertTrue(firstNotif.body.contains("Programming Fundamentals"))
        assertTrue(firstNotif.body.contains("Room R110"))

        assertTrue(secondNotif.fireAtIso.contains("09:50:00"))
        assertTrue(secondNotif.body.contains("Quantitative Reasoning-I"))
    }

    @Test
    fun `leadMinutes 15 schedules 07-45 and 09-45`() {
        val data = createSampleData()
        val now = getMondayAt(6, 0)
        val settings = NotificationSettings(enabled = true, leadMinutes = 15)

        val list = NotificationScheduler.buildSchedule(data, "BSCS-1-R", settings, now, horizonDays = 1)
        val mondayReminders = list.filter { it.type == NotificationType.LECTURE_REMINDER }

        assertEquals(2, mondayReminders.size)
        assertTrue(mondayReminders[0].fireAtIso.contains("07:45:00"))
        assertTrue(mondayReminders[1].fireAtIso.contains("09:45:00"))
    }

    @Test
    fun `now set to Monday 09-00 AM skips past 07-50 notification`() {
        val data = createSampleData()
        val now = getMondayAt(9, 0) // Monday 09:00 AM
        val settings = NotificationSettings(enabled = true, leadMinutes = 10)

        val list = NotificationScheduler.buildSchedule(data, "BSCS-1-R", settings, now, horizonDays = 1)
        val mondayReminders = list.filter { it.type == NotificationType.LECTURE_REMINDER }

        assertEquals(1, mondayReminders.size)
        assertTrue(mondayReminders[0].fireAtIso.contains("09:50:00"))
    }

    @Test
    fun `disabled settings produces empty list`() {
        val data = createSampleData()
        val now = getMondayAt(6, 0)
        val settings = NotificationSettings(enabled = false, leadMinutes = 10)

        val list = NotificationScheduler.buildSchedule(data, "BSCS-1-R", settings, now, horizonDays = 7)
        assertTrue(list.isEmpty())
    }

    @Test
    fun `changing class produces different notification ids and list`() {
        val data = createSampleData()
        val now = getMondayAt(6, 0)
        val settings = NotificationSettings(enabled = true, leadMinutes = 10)

        val bscsList = NotificationScheduler.buildSchedule(data, "BSCS-1-R", settings, now, horizonDays = 1)
        val bsitList = NotificationScheduler.buildSchedule(data, "BSIT-1-R", settings, now, horizonDays = 1)

        assertEquals(2, bscsList.size)
        assertEquals(1, bsitList.size)
        assertNotEquals(bscsList[0].id, bsitList[0].id)
    }
}
