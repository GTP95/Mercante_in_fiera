package com.example.mercanteinfiera.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mercante_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PLAYER_NAME = "player_name"
    }

    fun getPlayerName(): String {
        return prefs.getString(KEY_PLAYER_NAME, "Tu (Giocatore)") ?: "Tu (Giocatore)"
    }

    fun savePlayerName(name: String) {
        prefs.edit().putString(KEY_PLAYER_NAME, name).apply()
    }
}