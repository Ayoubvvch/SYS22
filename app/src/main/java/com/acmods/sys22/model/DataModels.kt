package com.acmods.sys22.model

import com.google.gson.annotations.SerializedName

/**
 * Data model for Critical Targets & Exams (stored in s22_criticalTargets)
 */
data class CriticalTarget(
    val id: String = "",
    val title: String = "",
    val date: String = "",
    val priority: String? = "STANDARD",
    val notes: String? = null
)

/**
 * Data model for Tasks & Schedule Blocks (stored in s22_tasks)
 */
data class TaskItem(
    val id: String = "",
    val title: String = "",
    val subject: String? = null,
    val mode: String? = null, // "timebox" | "pomodoro"
    val blockType: String? = null, // "logistic" | "focus"
    val isSchool: Boolean? = false,
    val isRecurring: Boolean? = false,
    val recurringDays: List<Int>? = null, // [1, 2, 3, 4, 5, 6, 7] (1=Monday ... 7=Sunday)
    val scheduledTime: String? = null, // "HH:mm"
    val targetEndTime: String? = null, // "HH:mm"
    val work: Int? = null,
    @SerializedName("break")
    val breakMinutes: Int? = null,
    val cycles: Int? = null,
    val notes: String? = null,
    val createdAt: String? = null
)

/**
 * Routine & Focus Settings
 */
data class ScheduleSettings(
    val wakeTime: String? = "03:30",
    val sleepTime: String? = "20:30",
    val totalFocusMinutes: Int? = 0,
    val goalMins: Int? = 180
)

/**
 * Data model for Custom Rules (stored in s22_rules)
 */
data class RuleItem(
    val id: Any? = null,
    val title: String = "",
    val text: String = "",
    val pinned: Boolean? = false
)

/**
 * Data model for Notes & Patterns (stored in s22_patterns)
 */
data class PatternItem(
    val id: Any? = null,
    val text: String = "",
    val pinned: Boolean? = false,
    val timestamp: String? = null
)

/**
 * Payload sent inside pending intents for alarms
 */
data class AlarmPayload(
    val notificationId: Int,
    val targetId: String,
    val type: String, // "EXAM", "SCHEDULE", "WAKE", "SLEEP", "FOCUS_DEFICIT"
    val title: String,
    val message: String,
    val channelId: String,
    val triggerTimeMillis: Long
)
