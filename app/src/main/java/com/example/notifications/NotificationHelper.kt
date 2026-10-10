package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {

    const val CHANNEL_ID = "lecture_reminders"
    const val CHANNEL_NAME = "Lecture Reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Reminders for upcoming lectures and timetable updates"
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleAll(context: Context, list: List<ScheduledNotification>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // First cancel all previous scheduled alarms if stored
        cancelAll(context, list)

        for (item in list) {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("notification_id", item.id)
                putExtra("title", item.title)
                putExtra("body", item.body)
                putExtra("lecture_key", item.lectureKey)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                item.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        item.fireAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        item.fireAtMillis,
                        pendingIntent
                    )
                }
            } catch (_: Exception) {
                // Fallback for missing exact alarm permission on Android 12+
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    item.fireAtMillis,
                    pendingIntent
                )
            }
        }
    }

    fun cancelAll(context: Context, list: List<ScheduledNotification>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (item in list) {
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                item.id,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    fun sendImmediateNotification(
        context: Context,
        title: String,
        body: String,
        notificationId: Int,
        lectureKey: String? = null
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (lectureKey != null) {
                putExtra("lecture_key", lectureKey)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission missing
        }
    }

    fun scheduleTestNotification(context: Context, leadMinutes: Int) {
        val testFireAtMillis = System.currentTimeMillis() + 5000L // 5 seconds from now
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("notification_id", 999999)
            putExtra("title", "Next lecture in $leadMinutes min")
            putExtra("body", "Operating Systems (Lab) | Room Computer Lab (CL) | Mr. Najaf Ali Khan | 09:00 to 10:00")
            putExtra("lecture_key", "test_sample")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            999999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                testFireAtMillis,
                pendingIntent
            )
        } catch (_: Exception) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                testFireAtMillis,
                pendingIntent
            )
        }
    }
}
