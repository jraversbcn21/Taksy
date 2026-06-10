package com.example.taksy.data.preferences

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    data class DailyReminderPrefs(
        val enabled: Boolean,
        val morningHour: Int,
        val morningMinute: Int,
        val eveningHour: Int,
        val eveningMinute: Int
    )

    private val themePrefs get() = context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
    private val languagePrefs get() = context.getSharedPreferences(LANGUAGE_PREFS, Context.MODE_PRIVATE)
    private val dailyReminderPrefs get() = context.getSharedPreferences(DAILY_REMINDER_PREFS, Context.MODE_PRIVATE)
    private val onboardingPrefs get() = context.getSharedPreferences(ONBOARDING_PREFS, Context.MODE_PRIVATE)

    // ── Theme ──────────────────────────────────────────────────────────────

    fun isDarkMode(): Boolean = themePrefs.getBoolean(KEY_DARK_MODE, false)

    fun setDarkMode(enabled: Boolean) {
        themePrefs.edit { putBoolean(KEY_DARK_MODE, enabled) }
    }

    // ── Language ───────────────────────────────────────────────────────────

    fun getLanguage(): String =
        languagePrefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE

    fun setLanguage(language: String) {
        languagePrefs.edit { putString(KEY_LANGUAGE, language) }
    }

    // ── Daily reminders ────────────────────────────────────────────────────

    fun loadDailyReminderPrefs(): DailyReminderPrefs = DailyReminderPrefs(
        enabled = dailyReminderPrefs.getBoolean(KEY_DAILY_ENABLED, false),
        morningHour = dailyReminderPrefs.getInt(KEY_MORNING_HOUR, 10),
        morningMinute = dailyReminderPrefs.getInt(KEY_MORNING_MINUTE, 0),
        eveningHour = dailyReminderPrefs.getInt(KEY_EVENING_HOUR, 18),
        eveningMinute = dailyReminderPrefs.getInt(KEY_EVENING_MINUTE, 0)
    )

    fun saveDailyReminderPrefs(prefs: DailyReminderPrefs) {
        dailyReminderPrefs.edit {
            putBoolean(KEY_DAILY_ENABLED, prefs.enabled)
            putInt(KEY_MORNING_HOUR, prefs.morningHour)
            putInt(KEY_MORNING_MINUTE, prefs.morningMinute)
            putInt(KEY_EVENING_HOUR, prefs.eveningHour)
            putInt(KEY_EVENING_MINUTE, prefs.eveningMinute)
        }
    }

    // ── Onboarding ─────────────────────────────────────────────────────────

    fun isOnboardingCompleted(): Boolean =
        onboardingPrefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    fun setOnboardingCompleted(completed: Boolean) {
        onboardingPrefs.edit { putBoolean(KEY_ONBOARDING_COMPLETED, completed) }
    }

    companion object {
        private const val THEME_PREFS = "theme_pref"
        private const val LANGUAGE_PREFS = "language_pref"
        private const val DAILY_REMINDER_PREFS = "daily_reminder_prefs"
        private const val ONBOARDING_PREFS = "onboarding_prefs"

        private const val KEY_DARK_MODE = "is_dark_mode"
        private const val KEY_LANGUAGE = "selected_language"
        private const val KEY_DAILY_ENABLED = "enabled"
        private const val KEY_MORNING_HOUR = "morning_hour"
        private const val KEY_MORNING_MINUTE = "morning_minute"
        private const val KEY_EVENING_HOUR = "evening_hour"
        private const val KEY_EVENING_MINUTE = "evening_minute"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

        private const val DEFAULT_LANGUAGE = "es"
    }
}
