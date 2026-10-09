package com.acmods.sys22.data

import android.content.Context
import android.content.SharedPreferences
import com.acmods.sys22.model.CriticalTarget
import com.acmods.sys22.model.TaskItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StorageManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "sys22_native_storage"
        private const val KEY_CRITICAL_TARGETS = "s22_criticalTargets"
        private const val KEY_TASKS = "s22_tasks"
        private const val KEY_WAKE_TIME = "s22_wakeTime"
        private const val KEY_SLEEP_TIME = "s22_sleepTime"
        private const val KEY_TOTAL_FOCUS = "s22_totalFocus"
        private const val KEY_GOAL_MINS = "s22_goalMins"
        private const val KEY_SCHEDULED_IDS = "s22_scheduled_alarm_ids"

        @Volatile
        private var instance: StorageManager? = null

        fun getInstance(context: Context): StorageManager {
            return instance ?: synchronized(this) {
                instance ?: StorageManager(context).also { instance = it }
            }
        }
    }

    fun saveCriticalTargetsJson(json: String) {
        prefs.edit().putString(KEY_CRITICAL_TARGETS, json).apply()
    }

    fun getCriticalTargets(): List<CriticalTarget> {
        val json = prefs.getString(KEY_CRITICAL_TARGETS, "[]") ?: "[]"
        return try {
            val type = object : TypeToken<List<CriticalTarget>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveTasksJson(json: String) {
        prefs.edit().putString(KEY_TASKS, json).apply()
    }

    fun getTasks(): List<TaskItem> {
        val json = prefs.getString(KEY_TASKS, "[]") ?: "[]"
        return try {
            val type = object : TypeToken<List<TaskItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setWakeTime(wakeTime: String) {
        prefs.edit().putString(KEY_WAKE_TIME, wakeTime).apply()
    }

    fun getWakeTime(): String {
        return prefs.getString(KEY_WAKE_TIME, "03:30") ?: "03:30"
    }

    fun setSleepTime(sleepTime: String) {
        prefs.edit().putString(KEY_SLEEP_TIME, sleepTime).apply()
    }

    fun getSleepTime(): String {
        return prefs.getString(KEY_SLEEP_TIME, "20:30") ?: "20:30"
    }

    fun setTotalFocus(focusMins: Int) {
        prefs.edit().putInt(KEY_TOTAL_FOCUS, focusMins).apply()
    }

    fun getTotalFocus(): Int {
        return prefs.getInt(KEY_TOTAL_FOCUS, 0)
    }

    fun setGoalMins(goalMins: Int) {
        prefs.edit().putInt(KEY_GOAL_MINS, goalMins).apply()
    }

    fun getGoalMins(): Int {
        return prefs.getInt(KEY_GOAL_MINS, 180)
    }

    fun saveActiveAlarmIds(ids: Set<Int>) {
        prefs.edit().putStringSet(KEY_SCHEDULED_IDS, ids.map { it.toString() }.toSet()).apply()
    }

    fun getActiveAlarmIds(): Set<Int> {
        val stringSet = prefs.getStringSet(KEY_SCHEDULED_IDS, emptySet()) ?: emptySet()
        return stringSet.mapNotNull { it.toIntOrNull() }.toSet()
    }
}
