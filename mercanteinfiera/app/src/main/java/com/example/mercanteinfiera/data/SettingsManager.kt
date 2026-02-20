package com.example.mercanteinfiera.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mercante_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PLAYER_NAME = "player_name"
        private const val KEY_THEME = "theme_mode"
        
        const val THEME_LIGHT = "Light"
        const val THEME_DARK = "Dark"
        const val THEME_SYSTEM = "System"
    }

    fun getPlayerName(): String {
        return prefs.getString(KEY_PLAYER_NAME, "Tu (Giocatore)") ?: "Tu (Giocatore)"
    }

    fun savePlayerName(name: String) {
        prefs.edit().putString(KEY_PLAYER_NAME, name).apply()
    }

    fun getThemeMode(): String {
        return prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
    }

    fun saveThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME, mode).apply()
    }
}