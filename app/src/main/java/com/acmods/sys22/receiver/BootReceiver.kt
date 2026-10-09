package com.acmods.sys22.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.acmods.sys22.scheduler.SYS22AlarmScheduler

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            // Restore all scheduled alarms from storage
            try {
                SYS22AlarmScheduler.scheduleAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
