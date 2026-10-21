package com.acmods.sys22.data

import android.content.Context
import android.content.SharedPreferences
import com.acmods.sys22.model.CriticalTarget
import com.acmods.sys22.model.PatternItem
import com.acmods.sys22.model.RuleItem
import com.acmods.sys22.model.TaskItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

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
        private const val KEY_RULES = "s22_rules"
        private const val KEY_MORNING_RULES = "s22_morningRules"
        private const val KEY_PATTERNS = "s22_patterns"

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

    fun saveRulesJson(json: String) {
        prefs.edit().putString(KEY_RULES, json).apply()
    }

    fun getRules(): List<RuleItem> {
        val json = prefs.getString(KEY_RULES, "[]") ?: "[]"
        return try {
            val type = object : TypeToken<List<RuleItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveMorningRulesJson(json: String) {
        prefs.edit().putString(KEY_MORNING_RULES, json).apply()
    }

    fun getMorningRules(): List<String> {
        val json = prefs.getString(KEY_MORNING_RULES, "[]") ?: "[]"
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun savePatternsJson(json: String) {
        prefs.edit().putString(KEY_PATTERNS, json).apply()
    }

    fun getPatterns(): List<PatternItem> {
        val json = prefs.getString(KEY_PATTERNS, "[]") ?: "[]"
        return try {
            val type = object : TypeToken<List<PatternItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Extracts Today's Wisdom from Rules, Morning Protocol, or Tactical Notes.
     * Rotates daily based on day of year, with fallback to elite tactical principles.
     */
    fun getTodayWisdom(): Pair<String, String> {
        val candidates = mutableListOf<Pair<String, String>>()

        // 1. Custom Rules
        val rules = getRules()
        for (rule in rules) {
            val text = rule.text.trim()
            if (text.isNotBlank()) {
                val title = if (rule.title.isNotBlank()) rule.title.trim() else "قاعدة انضباط"
                candidates.add(Pair(title, text))
            }
        }

        // 2. Morning Rules (Protocol)
        val morningRules = getMorningRules()
        for (mr in morningRules) {
            val text = mr.trim()
            if (text.isNotBlank()) {
                candidates.add(Pair("بروتوكول الصباح", text))
            }
        }

        // 3. Notes / Patterns (Logs)
        val patterns = getPatterns()
        for (pattern in patterns) {
            val rawText = pattern.text.trim()
            if (rawText.isNotBlank()) {
                // Strip markdown headers and bullets for clean notification display
                val cleanedText = rawText
                    .replace(Regex("(?m)^#+\\s*"), "")
                    .replace(Regex("(?m)^[-*]\\s*"), "")
                    .replace(Regex("\\*\\*|__|`"), "")
                    .trim()
                if (cleanedText.isNotBlank()) {
                    val excerpt = if (cleanedText.length > 160) {
                        cleanedText.take(157) + "..."
                    } else {
                        cleanedText
                    }
                    candidates.add(Pair("ملاحظة تكتيكية", excerpt))
                }
            }
        }

        // 4. Tactical Fallback Pool (if user has not added any rules or notes yet)
        if (candidates.isEmpty()) {
            candidates.addAll(listOf(
                Pair("السيادة المطلقة", "أنت لست جسدك، أنت المراقب المتحكم. الجسد آلة بيولوجية يجب ترويضها بالانضباط والعمل المستمر."),
                Pair("قاعدة البداية الباكرة", "الاستيقاظ والتركيز بينما العالم نيام يمنحك أفضلية تكتيكية حاسمة للسيطرة على يومك."),
                Pair("التركيز العميق", "التركيز ليس مجرد قول نعم للهدف، بل الشجاعة في قول لا لمئات المغريات والمشتتات اليومية."),
                Pair("قوة الاستمرارية", "النتائج الاستثنائية في الامتحانات لا تأتي من طفرة يوم واحد، بل من تراكم ساعات التركيز اليومية."),
                Pair("إدارة الطاقة", "استغل فترات ذروة طاقتك الذهنية في أصعب المواد، ولا تساوم أبداً في وقت راحتك ونومك الكافي."),
                Pair("عقلية الفوز", "الظروف لا تصنعك، استجابتك للظروف هي التي تحدد نتيجتك النهائية. لا تفاوض مع الكسل.")
            ))
        }

        // Rotate deterministically every day
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val index = (dayOfYear + 7) % candidates.size
        return candidates[index]
    }
}
