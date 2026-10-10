package com.example.util

import java.util.Calendar
import java.util.TimeZone

object FormatUtils {
    fun getOrdinal(number: Int): String {
        return when (number) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }

    fun parseMinutes(time: String): Int {
        return try {
            val parts = time.trim().split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (_: Exception) {
            0
        }
    }

    fun getTomorrowDayName(): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Karachi"))
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> "Monday"
        }
    }
}
