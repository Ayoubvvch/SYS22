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

    fun scheduleAll(context: Context) {
        val storage = StorageManager.getInstance(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 1. Cancel previous alarms
        val previousAlarmIds = storage.getActiveAlarmIds()
        for (id in previousAlarmIds) {
            cancelAlarm(context, alarmManager, id)
        }

        val activeAlarmIds = mutableSetOf<Int>()
        val now = System.currentTimeMillis()

        // 2. Schedule Critical Targets / Exams
        val targets = storage.getCriticalTargets()
        for (target in targets) {
            scheduleTargetTiers(context, alarmManager, target, now, activeAlarmIds)
        }

        // 3. Schedule Recurring Timetable Classes and Tasks
        val tasks = storage.getTasks()
        for (task in tasks) {
            scheduleTaskReminders(context, alarmManager, task, now, activeAlarmIds)
        }

        // 4. Schedule Wake & Sleep Protocols
        scheduleRoutineReminders(context, alarmManager, storage, now, activeAlarmIds)

        // 5. Schedule Daily Focus Checkpoint
        scheduleFocusCheckpoint(context, alarmManager, now, activeAlarmIds)

        // Save active IDs for future cleanup
        storage.saveActiveAlarmIds(activeAlarmIds)
    }

    private fun scheduleTargetTiers(
        context: Context,
        alarmManager: AlarmManager,
        target: CriticalTarget,
        now: Long,
        activeIds: MutableSet<Int>
    ) {
        val examTime = parseDateTime(target.date) ?: return
        if (examTime <= now) return // Past target

        val priorityTag = target.priority?.uppercase() ?: "CRITICAL"

        // Tier D-7 (7 days before)
        val d7 = examTime - 7L * 24 * 3600 * 1000
        if (d7 > now) {
            val id = generateAlarmId("CT_D7", target.id)
            scheduleExactAlarm(
                context, alarmManager, id, d7,
                targetId = target.id,
                type = "EXAM",
                title = "⚡ TACTICAL WARNING: D-7 TO TARGET",
                message = "[${target.title}] is approaching in 7 days. Threat Level: $priorityTag.",
                channelId = NotificationHelper.CHANNEL_EXAMS
            )
            activeIds.add(id)
        }

        // Tier D-3 (3 days before)
        val d3 = examTime - 3L * 24 * 3600 * 1000
        if (d3 > now) {
            val id = generateAlarmId("CT_D3", target.id)
            scheduleExactAlarm(
                context, alarmManager, id, d3,
                targetId = target.id,
                type = "EXAM",
                title = "🚨 CRITICAL WINDOW: D-3",
                message = "72 hours remaining for [${target.title}]. Review tactical intel notes.",
                channelId = NotificationHelper.CHANNEL_EXAMS
            )
            activeIds.add(id)
        }

        // Tier D-1 (1 day before)
        val d1 = examTime - 24L * 3600 * 1000
        if (d1 > now) {
            val id = generateAlarmId("CT_D1", target.id)
            scheduleExactAlarm(
                context, alarmManager, id, d1,
                targetId = target.id,
                type = "EXAM",
                title = "⚔️ POINT OF NO RETURN: TOMORROW",
                message = "Tomorrow is engagement day for [${target.title}]. Execute final review protocol.",
                channelId = NotificationHelper.CHANNEL_EXAMS
            )
            activeIds.add(id)
        }

        // Tier T-2h (2 hours before)
        val t2h = examTime - 2L * 3600 * 1000
        if (t2h > now) {
            val id = generateAlarmId("CT_T2H", target.id)
            scheduleExactAlarm(
                context, alarmManager, id, t2h,
                targetId = target.id,
                type = "EXAM",
                title = "🎯 TARGET ENGAGEMENT: T-2 HOURS",
                message = "Target [${target.title}] starts in 2 hours. Prime mental focus.",
                channelId = NotificationHelper.CHANNEL_EXAMS
            )
            activeIds.add(id)
        }

        // Tier T-30m (30 minutes before)
        val t30m = examTime - 30L * 60 * 1000
        if (t30m > now) {
            val id = generateAlarmId("CT_T30M", target.id)
            scheduleExactAlarm(
                context, alarmManager, id, t30m,
                targetId = target.id,
                type = "EXAM",
                title = "⚡ ENGAGEMENT IMMINENT: T-30 MINS",
                message = "30 minutes until [${target.title}]. Enter target execution zone.",
                channelId = NotificationHelper.CHANNEL_EXAMS
            )
            activeIds.add(id)
        }
    }

    private fun scheduleTaskReminders(
        context: Context,
        alarmManager: AlarmManager,
        task: TaskItem,
        now: Long,
        activeIds: MutableSet<Int>
    ) {
        val timeStr = task.scheduledTime ?: return
        val parts = timeStr.split(":").mapNotNull { it.toIntOrNull() }
        if (parts.size != 2) return
        val hour = parts[0]
        val minute = parts[1]

        if (task.isRecurring == true && !task.recurringDays.isNullOrEmpty()) {
            // Recurring schedule (e.g. school schedule Mon-Sat)
            for (dayIdx in task.recurringDays) {
                val nextTrigger = getNextOccurrence(dayIdx, hour, minute, -10)
                if (nextTrigger > now) {
                    val id = generateAlarmId("TASK_REC_${dayIdx}", task.id)
                    val label = if (task.isSchool == true) "School Timetable" else "Scheduled Block"
                    scheduleExactAlarm(
                        context, alarmManager, id, nextTrigger,
                        targetId = task.id,
                        type = "SCHEDULE",
                        title = "📅 $label: ${task.title}",
                        message = "Starts in 10 minutes at $timeStr. Prepare materials.",
                        channelId = NotificationHelper.CHANNEL_SCHEDULE
                    )
                    activeIds.add(id)
                }
            }
        } else {
            // One-off scheduled task for today
            val todayTrigger = getTodayOccurrence(hour, minute, -10)
            if (todayTrigger > now) {
                val id = generateAlarmId("TASK_ONE", task.id)
                scheduleExactAlarm(
                    context, alarmManager, id, todayTrigger,
                    targetId = task.id,
                    type = "SCHEDULE",
                    title = "📋 MISSION SCHEDULED: ${task.title}",
                    message = "Starts in 10 minutes at $timeStr.",
                    channelId = NotificationHelper.CHANNEL_SCHEDULE
                )
                activeIds.add(id)
            }
        }
    }

    private fun scheduleRoutineReminders(
        context: Context,
        alarmManager: AlarmManager,
        storage: StorageManager,
        now: Long,
        activeIds: MutableSet<Int>
    ) {
        // 1. Wake Time Reminder
        val wakeStr = storage.getWakeTime()
        val wakeParts = wakeStr.split(":").mapNotNull { it.toIntOrNull() }
        if (wakeParts.size == 2) {
            val wakeTrigger = getNextDailyOccurrence(wakeParts[0], wakeParts[1], 0)
            if (wakeTrigger > now) {
                val id = generateAlarmId("ROUTINE", "WAKE")
                scheduleExactAlarm(
                    context, alarmManager, id, wakeTrigger,
                    targetId = "wake_routine",
                    type = "WAKE",
                    title = "☀️ SYSTEM AWAKENING: INITIATE MORNING RULES",
                    message = "System wake time reached ($wakeStr). Execute morning protocols & hydrate.",
                    channelId = NotificationHelper.CHANNEL_ROUTINES
                )
                activeIds.add(id)
            }
        }

        // 2. Sleep Time Reminder (T-30 mins)
        val sleepStr = storage.getSleepTime()
        val sleepParts = sleepStr.split(":").mapNotNull { it.toIntOrNull() }
        if (sleepParts.size == 2) {
            val sleepTrigger = getNextDailyOccurrence(sleepParts[0], sleepParts[1], -30)
            if (sleepTrigger > now) {
                val id = generateAlarmId("ROUTINE", "SLEEP_T30")
                scheduleExactAlarm(
                    context, alarmManager, id, sleepTrigger,
                    targetId = "sleep_routine",
                    type = "SLEEP",
                    title = "🌙 SLEEP CYCLE PREPARATION: T-30 MINS",
                    message = "Approaching sleep cycle ($sleepStr). Terminate screens & review daily logs.",
                    channelId = NotificationHelper.CHANNEL_ROUTINES
                )
                activeIds.add(id)
            }
        }
    }

    private fun scheduleFocusCheckpoint(
        context: Context,
        alarmManager: AlarmManager,
        now: Long,
        activeIds: MutableSet<Int>
    ) {
        // Daily evaluation at 18:00
        val checkTrigger = getNextDailyOccurrence(18, 0, 0)
        if (checkTrigger > now) {
            val id = generateAlarmId("DISCIPLINE", "FOCUS_CHECK_18")
            scheduleExactAlarm(
                context, alarmManager, id, checkTrigger,
                targetId = "focus_checkpoint",
                type = "FOCUS_CHECK",
                title = "⚡ FOCUS LOAD EVALUATION",
                message = "Daily checkpoint: Check your remaining focus hours to hit today's target.",
                channelId = NotificationHelper.CHANNEL_DISCIPLINE
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
        val id = generateAlarmId("SNOOZE", targetId + System.currentTimeMillis())

        scheduleExactAlarm(
            context, alarmManager, id, triggerTime,
            targetId = targetId,
            type = "SNOOZE",
            title = "⏰ SNOOZED: $title",
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

    private fun getNextDailyOccurrence(hour: Int, minute: Int, offsetMinutes: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, offsetMinutes)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun getTodayOccurrence(hour: Int, minute: Int, offsetMinutes: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, offsetMinutes)
        }
        return cal.timeInMillis
    }

    /**
     * Map day index: 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat, 7=Sun
     * to Java Calendar.DAY_OF_WEEK
     */
    private fun getNextOccurrence(dayIndex: Int, hour: Int, minute: Int, offsetMinutes: Int): Long {
        val targetDayOfWeek = when (dayIndex) {
            1 -> Calendar.MONDAY
            2 -> Calendar.TUESDAY
            3 -> Calendar.WEDNESDAY
            4 -> Calendar.THURSDAY
            5 -> Calendar.FRIDAY
            6 -> Calendar.SATURDAY
            7 -> Calendar.SUNDAY
            else -> Calendar.MONDAY
        }

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, offsetMinutes)
        }

        val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        var daysToAdd = (targetDayOfWeek - currentDayOfWeek) % 7
        if (daysToAdd < 0) daysToAdd += 7

        if (daysToAdd == 0 && cal.timeInMillis <= System.currentTimeMillis()) {
            daysToAdd = 7
        }

        cal.add(Calendar.DAY_OF_YEAR, daysToAdd)
        return cal.timeInMillis
    }
}
