package com.acmods.sys22.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.acmods.sys22.notification.NotificationHelper
import com.acmods.sys22.scheduler.SYS22AlarmScheduler

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val helper = NotificationHelper(context)
        val notifId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, 0)

        when (intent.action) {
            NotificationHelper.ACTION_SNOOZE -> {
                val targetId = intent.getStringExtra(NotificationHelper.EXTRA_TARGET_ID) ?: ""
                val title = intent.getStringExtra(NotificationHelper.EXTRA_TITLE) ?: "Reminder"
                val message = intent.getStringExtra(NotificationHelper.EXTRA_MESSAGE) ?: ""
                val channelId = intent.getStringExtra(NotificationHelper.EXTRA_CHANNEL_ID) ?: NotificationHelper.CHANNEL_EXAMS

                helper.cancelNotification(notifId)
                SYS22AlarmScheduler.scheduleSnooze(context, targetId, title, message, channelId, 15)
                Toast.makeText(context, "SYS22: Alert snoozed for 15 minutes", Toast.LENGTH_SHORT).show()
            }
            NotificationHelper.ACTION_DISMISS -> {
                helper.cancelNotification(notifId)
            }
        }
    }
}
