package com.berat.sakus.theme

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    // Using true as default to match Flutter implementation
    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("isDark", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        try {
            val newValue = !_isDarkTheme.value
            _isDarkTheme.value = newValue
            prefs.edit().putBoolean("isDark", newValue).apply()
        } catch (e: Exception) {
            Log.e("ThemeManager", "Error saving theme: ${e.message}")
        }
    }

    val isDark: Boolean
        get() = _isDarkTheme.value

    companion object {
        @Volatile
        private var INSTANCE: ThemeManager? = null

        fun getInstance(context: Context): ThemeManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThemeManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
