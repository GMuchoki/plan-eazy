package com.nesh.planeazy.util

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("planeazy_prefs", Context.MODE_PRIVATE)

    fun setDarkMode(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun isDarkMode(): Boolean {
        return sharedPreferences.getBoolean("dark_mode", false)
    }

    fun setCurrency(currency: String) {
        sharedPreferences.edit().putString("currency", currency).apply()
    }

    fun getCurrency(): String {
        return sharedPreferences.getString("currency", "KES") ?: "KES"
    }

    fun setUseBiometrics(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("use_biometrics", enabled).apply()
    }

    fun useBiometrics(): Boolean {
        return sharedPreferences.getBoolean("use_biometrics", false)
    }

    // Notification Preferences
    fun setNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("notifications_enabled", enabled).apply()
    }

    fun areNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean("notifications_enabled", true)
    }

    fun setBudgetAlertsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("budget_alerts_enabled", enabled).apply()
    }

    fun areBudgetAlertsEnabled(): Boolean {
        return sharedPreferences.getBoolean("budget_alerts_enabled", true)
    }

    fun setGoalMilestonesEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("goal_milestones_enabled", enabled).apply()
    }

    fun areGoalMilestonesEnabled(): Boolean {
        return sharedPreferences.getBoolean("goal_milestones_enabled", true)
    }

    fun setReminderEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("reminder_enabled", enabled).apply()
    }

    fun isReminderEnabled(): Boolean {
        return sharedPreferences.getBoolean("reminder_enabled", true)
    }
}
