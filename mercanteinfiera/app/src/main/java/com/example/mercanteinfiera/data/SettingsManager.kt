package com.example.mercanteinfiera.data

import android.content.Context
import android.content.SharedPreferences
import com.example.mercanteinfiera.R

class SettingsManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mercante_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PLAYER_NAME = "player_name"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_LANGUAGE = "language"
        
        const val THEME_LIGHT = "Light"
        const val THEME_DARK = "Dark"
        const val THEME_SYSTEM = "System"

        const val LANG_IT = "it"
        const val LANG_EN = "en"
    }

    fun getPlayerName(): String {
        val defaultName = context.getString(R.string.default_player_name)
        return prefs.getString(KEY_PLAYER_NAME, defaultName) ?: defaultName
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

    fun getLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, LANG_IT) ?: LANG_IT
    }

    fun saveLanguage(lang: String) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
    }
}