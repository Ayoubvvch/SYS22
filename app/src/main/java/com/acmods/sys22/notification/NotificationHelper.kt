package com.acmods.sys22.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.acmods.sys22.MainActivity
import com.acmods.sys22.R
import com.acmods.sys22.receiver.NotificationActionReceiver

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_EXAMS = "sys22_exams_channel"
        const val CHANNEL_TASKS = "sys22_tasks_channel"

        const val EXTRA_NOTIFICATION_ID = "extra_notif_id"
        const val EXTRA_TARGET_ID = "extra_target_id"
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_CHANNEL_ID = "extra_channel_id"

        const val ACTION_SNOOZE = "com.acmods.sys22.ACTION_SNOOZE"
        const val ACTION_DISMISS = "com.acmods.sys22.ACTION_DISMISS"

        // Fixed slot ID for tasks to prevent notification drawer flooding
        const val NOTIFICATION_ID_TASK_SLOT = 2000
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 1. Critical Exams & Targets (High importance, alert sound, vibration)
            val examChannel = NotificationChannel(
                CHANNEL_EXAMS,
                "الامتحانات والمواعيد الحاسمة (Exams)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات الامتحانات الهامة فقط (قبل الامتحان بيوم وقبل موعده بـ 30 دقيقة)."
                enableLights(true)
                lightColor = Color.parseColor("#ef4444")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 180, 80, 180)
                setShowBadge(true)
            }

            // 2. Scheduled Tasks (Exact time reminders)
            val taskChannel = NotificationChannel(
                CHANNEL_TASKS,
                "المهام المجدولة (Scheduled Tasks)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات المهام المحددة بوقت معين بالضبط دون تكرار مزعج."
                enableLights(true)
                lightColor = Color.parseColor("#38bdf8")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 120, 60, 120)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(listOf(examChannel, taskChannel))
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
        // Tap Intent -> Opens SYS22
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

        // Snooze 10-min Action Intent
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

        val accentColor = if (channelId == CHANNEL_EXAMS) {
            Color.parseColor("#ef4444")
        } else {
            Color.parseColor("#38bdf8")
        }

        val largeIcon = try {
            BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
        } catch (_: Exception) { null }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .apply {
                if (largeIcon != null) {
                    setLargeIcon(largeIcon)
                }
            }
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setColor(accentColor)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            // Auto timeout after 45 minutes so stale notifications do not clutter the bar!
            .setTimeoutAfter(45 * 60 * 1000L)
            .addAction(android.R.drawable.ic_popup_sync, "تأجيل 10 د", snoozePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إغلاق", dismissPendingIntent)

        notificationManager.notify(notificationId, builder.build())
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
}
