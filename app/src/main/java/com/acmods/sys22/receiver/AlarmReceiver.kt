package com.acmods.sys22.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.acmods.sys22.data.StorageManager
import com.acmods.sys22.notification.NotificationHelper
import com.acmods.sys22.scheduler.SYS22AlarmScheduler

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, 0)
        val targetId = intent.getStringExtra(NotificationHelper.EXTRA_TARGET_ID) ?: ""
        val type = intent.getStringExtra(NotificationHelper.EXTRA_TYPE) ?: "REMINDER"
        val title = intent.getStringExtra(NotificationHelper.EXTRA_TITLE) ?: "SYS22"
        val message = intent.getStringExtra(NotificationHelper.EXTRA_MESSAGE) ?: ""
        val channelId = intent.getStringExtra(NotificationHelper.EXTRA_CHANNEL_ID) ?: NotificationHelper.CHANNEL_TASKS

        if (type == "WISDOM") {
            val storage = StorageManager.getInstance(context)
            val (wisdomTitle, wisdomText) = storage.getTodayWisdom()
            val helper = NotificationHelper(context)
            helper.showNotification(
                notificationId = NotificationHelper.NOTIFICATION_ID_WISDOM,
                targetId = "daily_wisdom",
                type = "WISDOM",
                title = "⚡ حكمة اليوم: $wisdomTitle",
                message = wisdomText,
                channelId = NotificationHelper.CHANNEL_WISDOM
            )
            // Automatically schedule next day's wisdom
            SYS22AlarmScheduler.scheduleNextDailyWisdom(context)
            return
        }

        val helper = NotificationHelper(context)
        helper.showNotification(
            notificationId = notificationId,
            targetId = targetId,
            type = type,
            title = title,
            message = message,
            channelId = channelId
        )
    }
}
