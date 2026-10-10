package com.example.notifications

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NotificationSettings(
    val enabled: Boolean = true,
    val leadMinutes: Int = 10,
    val timetableUpdatesAlert: Boolean = true,
    val morningSummaryEnabled: Boolean = false,
    val morningSummaryTime: String = "07:00",
    val lastPermissionPromptTime: Long = 0L,
    val hasAskedPermission: Boolean = false
)

enum class NotificationType {
    LECTURE_REMINDER,
    TIMETABLE_UPDATE,
    MORNING_SUMMARY
}

data class ScheduledNotification(
    val id: Int,
    val fireAtMillis: Long,
    val fireAtIso: String,
    val title: String,
    val body: String,
    val lectureKey: String,
    val type: NotificationType = NotificationType.LECTURE_REMINDER
)
