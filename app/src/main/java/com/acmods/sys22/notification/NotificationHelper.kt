package com.acmods.sys22.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.acmods.sys22.MainActivity
import com.acmods.sys22.R
import com.acmods.sys22.receiver.NotificationActionReceiver

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_EXAMS = "sys22_exams_channel"
        const val CHANNEL_SCHEDULE = "sys22_schedule_channel"
        const val CHANNEL_ROUTINES = "sys22_routines_channel"
        const val CHANNEL_DISCIPLINE = "sys22_discipline_channel"

        const val EXTRA_NOTIFICATION_ID = "extra_notif_id"
        const val EXTRA_TARGET_ID = "extra_target_id"
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_CHANNEL_ID = "extra_channel_id"

        const val ACTION_SNOOZE = "com.acmods.sys22.ACTION_SNOOZE"
        const val ACTION_DISMISS = "com.acmods.sys22.ACTION_DISMISS"
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 1. Critical Targets & Exams (Max urgency, heads-up)
            val examChannel = NotificationChannel(
                CHANNEL_EXAMS,
                "Critical Targets & Exams",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Tiered countdown reminders for upcoming exams and major milestones."
                enableLights(true)
                lightColor = Color.parseColor("#ef4444") // Danger Red
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200, 100, 400)
                setShowBadge(true)
            }

            // 2. Tactical Schedule & Timetable (High urgency)
            val scheduleChannel = NotificationChannel(
                CHANNEL_SCHEDULE,
                "Tactical Schedule & Classes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for scheduled timetable classes and focus sessions."
                enableLights(true)
                lightColor = Color.parseColor("#38bdf8") // Cold Blue
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 150, 100, 150)
                setShowBadge(true)
            }

            // 3. Wake & Sleep Protocols (Default urgency)
            val routineChannel = NotificationChannel(
                CHANNEL_ROUTINES,
                "Wake & Sleep Protocols",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Morning awakening protocol and night wind-down alerts."
                enableLights(true)
                lightColor = Color.parseColor("#eab308") // Amber
                setShowBadge(true)
            }

            // 4. Discipline & Focus Deficit (High urgency)
            val disciplineChannel = NotificationChannel(
                CHANNEL_DISCIPLINE,
                "Focus & Discipline Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Tactical alerts for daily focus goals and streak maintenance."
                enableLights(true)
                lightColor = Color.parseColor("#a855f7") // Purple
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(
                listOf(examChannel, scheduleChannel, routineChannel, disciplineChannel)
            )
        }
    }

    fun showNotification(
        notificationId: Int,
        targetId: String,
        type: String,
        title: String,
        message: String,
        channelId: String
    ) {
        // Main Click Intent -> Opens SYS22 MainActivity
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TARGET_ID, targetId)
            putExtra(EXTRA_TYPE, type)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze 15-min Action Intent
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_TARGET_ID, targetId)
            putExtra(EXTRA_TYPE, type)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_CHANNEL_ID, channelId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss Action Intent
        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val accentColor = when (channelId) {
            CHANNEL_EXAMS -> Color.parseColor("#ef4444")
            CHANNEL_SCHEDULE -> Color.parseColor("#38bdf8")
            CHANNEL_ROUTINES -> Color.parseColor("#eab308")
            else -> Color.parseColor("#06b6d4")
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setColor(accentColor)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(
                if (type == "EXAM" || type == "SCHEDULE") NotificationCompat.CATEGORY_ALARM
                else NotificationCompat.CATEGORY_REMINDER
            )

        // Add interactive actions
        if (type == "EXAM" || type == "SCHEDULE") {
            builder.addAction(
                android.R.drawable.ic_popup_sync,
                "SNOOZE 15M",
                snoozePendingIntent
            )
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "DISMISS",
                dismissPendingIntent
            )
        }

        notificationManager.notify(notificationId, builder.build())
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
}
