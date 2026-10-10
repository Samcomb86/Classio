package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra("notification_id", 0)
        val title = intent.getStringExtra("title") ?: "Lecture Reminder"
        val body = intent.getStringExtra("body") ?: "You have an upcoming class."
        val lectureKey = intent.getStringExtra("lecture_key")

        NotificationHelper.sendImmediateNotification(
            context = context,
            title = title,
            body = body,
            notificationId = notificationId,
            lectureKey = lectureKey
        )
    }
}
