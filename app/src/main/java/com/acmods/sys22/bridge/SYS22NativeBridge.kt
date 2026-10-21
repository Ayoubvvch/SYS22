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

            jsonObject.get("rules")?.let {
                val str = if (it.isJsonPrimitive) it.asString else it.toString()
                storage.saveRulesJson(str)
            }

            jsonObject.get("morningRules")?.let {
                val str = if (it.isJsonPrimitive) it.asString else it.toString()
                storage.saveMorningRulesJson(str)
            }

            jsonObject.get("patterns")?.let {
                val str = if (it.isJsonPrimitive) it.asString else it.toString()
                storage.savePatternsJson(str)
            }

            jsonObject.get("wakeTime")?.let {
                val str = if (it.isJsonPrimitive) it.asString else it.toString()
                storage.setWakeTime(str)
            }

            // Schedule only essential exam, task & daily wisdom alerts
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
                "s22_rules" -> storage.saveRulesJson(value)
                "s22_morningRules" -> storage.saveMorningRulesJson(value)
                "s22_patterns" -> storage.savePatternsJson(value)
                "s22_wakeTime" -> storage.setWakeTime(value)
            }
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
                title = "📌 تجربة إشعار امتحان",
                message = "هذا نموذج للإشعار التكتيكي المنضبط عند اقتراب موعد الامتحان أو المهمة.",
                channelId = NotificationHelper.CHANNEL_EXAMS
            )
            Toast.makeText(context, "SYS22: تم إرسال الإشعار التجريبي", Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun isNotificationPermissionGranted(): Boolean {
        return activity.isNotificationPermissionGranted()
    }

    @JavascriptInterface
    fun checkForUpdates() {
        activity.runOnUiThread {
            com.acmods.sys22.update.UpdateManager.checkForUpdates(activity, userInitiated = true)
        }
    }

    @JavascriptInterface
    fun triggerCloudSync() {
        activity.evaluateJs("window.sys22ForceCloudSync && window.sys22ForceCloudSync();")
    }

    @JavascriptInterface
    fun testWisdomNotification() {
        activity.runOnUiThread {
            val (wisdomTitle, wisdomText) = storage.getTodayWisdom()
            val helper = NotificationHelper(context)
            helper.showNotification(
                notificationId = NotificationHelper.NOTIFICATION_ID_WISDOM,
                targetId = "test_wisdom",
                type = "WISDOM",
                title = "⚡ حكمة اليوم: $wisdomTitle",
                message = wisdomText,
                channelId = NotificationHelper.CHANNEL_WISDOM
            )
            Toast.makeText(context, "SYS22: تم إرسال إشعار حكمة اليوم", Toast.LENGTH_SHORT).show()
        }
    }
}
