package com.acmods.sys22.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import com.acmods.sys22.data.StorageManager
import com.acmods.sys22.model.CriticalTarget
import com.acmods.sys22.model.TaskItem
import com.acmods.sys22.notification.NotificationHelper
import com.acmods.sys22.receiver.AlarmReceiver
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

object SYS22AlarmScheduler {

    const val ACTION_FIRE_ALARM = "com.acmods.sys22.ACTION_FIRE_ALARM"

    /**
     * Schedules ONLY the most critical alerts:
     * 1. Important exams (1 day before, and 30 minutes before).
     * 2. User tasks with exact scheduled times for today.
     * No background spam, no routine noise, no repetitive notifications.
     */
    @Synchronized
    fun scheduleAll(context: Context) {
        val storage = StorageManager.getInstance(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 1. Cancel previously active alarms
        val previousAlarmIds = storage.getActiveAlarmIds()
        for (id in previousAlarmIds) {
            cancelAlarm(context, alarmManager, id)
        }

        val activeAlarmIds = mutableSetOf<Int>()
        val now = System.currentTimeMillis()

        // 2. Schedule Exams / Critical Targets (Max 2 clean alerts per exam)
        val targets = storage.getCriticalTargets()
        for (target in targets) {
            scheduleExamAlerts(context, alarmManager, target, now, activeAlarmIds)
        }

        // 3. Schedule Exact User Tasks for Today (Excludes fixed school timetables)
        val tasks = storage.getTasks()
        for (task in tasks) {
            scheduleExactUserTask(context, alarmManager, task, now, activeAlarmIds)
        }

        // Save active IDs
        storage.saveActiveAlarmIds(activeAlarmIds)
    }

    private fun scheduleExamAlerts(
        context: Context,
        alarmManager: AlarmManager,
        target: CriticalTarget,
        now: Long,
        activeIds: MutableSet<Int>
    ) {
        val examTime = parseDateTime(target.date) ?: return
        if (examTime <= now + 60_000) return // Past or already starting

        val cleanTitle = target.title.trim()

        // Alert 1: Day Before (D-1) — 24 hours before exam
        val d1 = examTime - 24L * 3600 * 1000
        if (d1 > now + 60_000) {
            val id = generateAlarmId("EXAM_D1", target.id)
            scheduleExactAlarm(
                context, alarmManager, id, d1,
                targetId = target.id,
                type = "EXAM",
                title = "📌 تذكير: امتحان غداً",
                message = "غداً موعد امتحان: $cleanTitle",
                channelId = NotificationHelper.CHANNEL_EXAMS
            )
            activeIds.add(id)
        }

        // Alert 2: 30 minutes before exam
        val t30m = examTime - 30L * 60 * 1000
        if (t30m > now + 60_000) {
            val id = generateAlarmId("EXAM_T30M", target.id)
            scheduleExactAlarm(
                context, alarmManager, id, t30m,
                targetId = target.id,
                type = "EXAM",
                title = "🚨 اقترب موعد الامتحان",
                message = "يبدأ امتحان [$cleanTitle] خلال 30 دقيقة",
                channelId = NotificationHelper.CHANNEL_EXAMS
            )
            activeIds.add(id)
        }
    }

    private fun scheduleExactUserTask(
        context: Context,
        alarmManager: AlarmManager,
        task: TaskItem,
        now: Long,
        activeIds: MutableSet<Int>
    ) {
        // Ignore background school template blocks and logistic fillers
        if (task.isSchool == true || task.blockType.equals("logistic", ignoreCase = true)) {
            return
        }

        val timeStr = task.scheduledTime?.trim() ?: return
        if (timeStr.isBlank()) return

        val parts = timeStr.split(":").mapNotNull { it.toIntOrNull() }
        if (parts.size != 2) return
        val hour = parts[0]
        val minute = parts[1]

        val calNow = Calendar.getInstance()
        val currentDayOfWeek = when (calNow.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }

        // If task is recurring, only schedule if today is in its recurringDays
        if (task.isRecurring == true) {
            val days = task.recurringDays ?: emptyList()
            if (!days.contains(currentDayOfWeek)) {
                return
            }
        }

        // Calculate exact trigger time for TODAY at HH:mm:00
        val targetCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val triggerTime = targetCal.timeInMillis
        // Only schedule if strictly in the future today (at least 30 seconds ahead)
        if (triggerTime > now + 30_000) {
            val id = generateAlarmId("TASK", task.id)
            val cleanTitle = task.title.trim()
            scheduleExactAlarm(
                context, alarmManager, id, triggerTime,
                targetId = task.id,
                type = "TASK",
                title = "⏰ موعد المهمة الآن",
                message = "حان الآن موعد: $cleanTitle ($timeStr)",
                channelId = NotificationHelper.CHANNEL_TASKS
            )
            activeIds.add(id)
        }
    }

    fun scheduleSnooze(
        context: Context,
        targetId: String,
        title: String,
        message: String,
        channelId: String,
        snoozeMinutes: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        val id = generateAlarmId("SNOOZE", targetId)

        scheduleExactAlarm(
            context, alarmManager, id, triggerTime,
            targetId = targetId,
            type = "SNOOZE",
            title = "⏰ تذكير مؤجل: $title",
            message = message,
            channelId = channelId
        )
    }

    private fun scheduleExactAlarm(
        context: Context,
        alarmManager: AlarmManager,
        alarmId: Int,
        triggerAtMillis: Long,
        targetId: String,
        type: String,
        title: String,
        message: String,
        channelId: String
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_FIRE_ALARM
            putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, alarmId)
            putExtra(NotificationHelper.EXTRA_TARGET_ID, targetId)
            putExtra(NotificationHelper.EXTRA_TYPE, type)
            putExtra(NotificationHelper.EXTRA_TITLE, title)
            putExtra(NotificationHelper.EXTRA_MESSAGE, message)
            putExtra(NotificationHelper.EXTRA_CHANNEL_ID, channelId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    AlarmManagerCompat.setExactAndAllowWhileIdle(
                        alarmManager,
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    AlarmManagerCompat.setAndAllowWhileIdle(
                        alarmManager,
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                AlarmManagerCompat.setExactAndAllowWhileIdle(
                    alarmManager,
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cancelAlarm(context: Context, alarmManager: AlarmManager, alarmId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_FIRE_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun generateAlarmId(prefix: String, key: String): Int {
        return abs("$prefix:$key".hashCode())
    }

    private fun parseDateTime(dateStr: String): Long? {
        val formats = arrayOf(
            "yyyy-MM-dd'T'HH:mm",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                val d = sdf.parse(dateStr)
                if (d != null) return d.time
            } catch (_: Exception) {}
        }
        return null
    }
}
