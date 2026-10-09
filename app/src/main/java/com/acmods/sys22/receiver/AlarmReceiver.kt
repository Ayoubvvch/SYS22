package com.acmods.sys22.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.acmods.sys22.notification.NotificationHelper
import com.acmods.sys22.scheduler.SYS22AlarmScheduler

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, 0)
        val targetId = intent.getStringExtra(NotificationHelper.EXTRA_TARGET_ID) ?: ""
        val type = intent.getStringExtra(NotificationHelper.EXTRA_TYPE) ?: "REMINDER"
        val title = intent.getStringExtra(NotificationHelper.EXTRA_TITLE) ?: "SYSTEM 22 Alert"
        val message = intent.getStringExtra(NotificationHelper.EXTRA_MESSAGE) ?: "Tactical notification"
        val channelId = intent.getStringExtra(NotificationHelper.EXTRA_CHANNEL_ID) ?: NotificationHelper.CHANNEL_EXAMS

        val helper = NotificationHelper(context)
        helper.showNotification(
            notificationId = notificationId,
            targetId = targetId,
            type = type,
            title = title,
            message = message,
            channelId = channelId
        )

        // For daily recurring protocols (WAKE/SLEEP/SCHEDULE), reschedule rolling next occurrences
        if (type == "WAKE" || type == "SLEEP" || type == "SCHEDULE") {
            try {
                SYS22AlarmScheduler.scheduleAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
