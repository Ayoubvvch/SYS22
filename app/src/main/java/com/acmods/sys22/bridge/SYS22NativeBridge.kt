package com.acmods.sys22.bridge

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.acmods.sys22.MainActivity
import com.acmods.sys22.data.StorageManager
import com.acmods.sys22.notification.NotificationHelper
import com.acmods.sys22.scheduler.SYS22AlarmScheduler
import com.google.gson.Gson
import com.google.gson.JsonObject

class SYS22NativeBridge(private val activity: MainActivity) {

    private val context: Context = activity.applicationContext
    private val storage = StorageManager.getInstance(context)
    private val gson = Gson()

    @JavascriptInterface
    fun syncAll(payloadJson: String) {
        try {
            val jsonObject = gson.fromJson(payloadJson, JsonObject::class.java)

            jsonObject.get("criticalTargets")?.let {
                val str = if (it.isJsonPrimitive) it.asString else it.toString()
                storage.saveCriticalTargetsJson(str)
            }

            jsonObject.get("tasks")?.let {
                val str = if (it.isJsonPrimitive) it.asString else it.toString()
                storage.saveTasksJson(str)
            }

            jsonObject.get("wakeTime")?.let {
                storage.setWakeTime(it.asString)
            }

            jsonObject.get("sleepTime")?.let {
                storage.setSleepTime(it.asString)
            }

            jsonObject.get("totalFocus")?.let {
                storage.setTotalFocus(it.asInt)
            }

            jsonObject.get("goalMins")?.let {
                storage.setGoalMins(it.asInt)
            }

            // Immediately schedule all system alarms in native AlarmManager
            SYS22AlarmScheduler.scheduleAll(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun onStorageUpdated(key: String, value: String) {
        try {
            when (key) {
                "s22_criticalTargets" -> storage.saveCriticalTargetsJson(value)
                "s22_tasks" -> storage.saveTasksJson(value)
                "s22_wakeTime" -> storage.setWakeTime(value.replace("\"", ""))
                "s22_sleepTime" -> storage.setSleepTime(value.replace("\"", ""))
                "s22_totalFocus" -> storage.setTotalFocus(value.toIntOrNull() ?: 0)
                "s22_goalMins" -> storage.setGoalMins(value.toIntOrNull() ?: 180)
            }

            // Reschedule whenever data changes
            SYS22AlarmScheduler.scheduleAll(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun requestNotificationPermission() {
        activity.runOnUiThread {
            activity.requestNotificationPermission()
        }
    }

    @JavascriptInterface
    fun triggerHaptic(type: String) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (type.lowercase()) {
                    "light" -> VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE)
                    "heavy" -> VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                    "success" -> VibrationEffect.createWaveform(longArrayOf(0, 30, 40, 60), -1)
                    "alert" -> VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100), -1)
                    else -> VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        } catch (_: Exception) {}
    }

    @JavascriptInterface
    fun testNotification() {
        activity.runOnUiThread {
            val helper = NotificationHelper(context)
            helper.showNotification(
                notificationId = 9999,
                targetId = "test_sys22",
                type = "EXAM",
                title = "⚡ SYS22: SYSTEM INTEGRATION ACTIVE",
                message = "Mobile Engine is 100% active. Tiered exam alerts, timetable reminders, and wake/sleep protocols are engaged.",
                channelId = NotificationHelper.CHANNEL_EXAMS
            )
            Toast.makeText(context, "SYS22: Test Alert Dispatched", Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun isNotificationPermissionGranted(): Boolean {
        return activity.isNotificationPermissionGranted()
    }
}
