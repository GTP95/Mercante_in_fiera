package eu.gtpware.mercanteinfiera.data

import android.content.Context
import android.content.SharedPreferences
import eu.gtpware.mercanteinfiera.R
import eu.gtpware.mercanteinfiera.models.DifficultyLevel
import java.util.Locale

class SettingsManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mercante_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PLAYER_NAME = "player_name"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_FIRST_STARTUP = "first_startup"
        private const val KEY_HAS_SEEN_TUTORIAL = "has_seen_tutorial"
        private const val KEY_DIFFICULTY_LEVEL = "difficulty_level"
        
        const val THEME_LIGHT = "Light"
        const val THEME_DARK = "Dark"
        const val THEME_SYSTEM = "System"

        const val LANG_IT = "it"
        const val LANG_EN = "en"
        const val LANG_DE = "de"
        const val LANG_FR = "fr"
        const val LANG_ES = "es"
        const val LANG_PT = "pt"
        const val LANG_NL = "nl"
        const val LANG_PL = "pl"
        const val LANG_SV = "sv"
        const val LANG_DA = "da"
        const val LANG_FI = "fi"
        const val LANG_EL = "el"
        const val LANG_CS = "cs"
        const val LANG_HU = "hu"
        const val LANG_RO = "ro"
        const val LANG_BG = "bg"
        const val LANG_SK = "sk"
        const val LANG_HR = "hr"
        const val LANG_LT = "lt"
        const val LANG_SL = "sl"
        const val LANG_LV = "lv"
        const val LANG_ET = "et"
        const val LANG_GA = "ga"
        const val LANG_MT = "mt"

        val SUPPORTED_LANGUAGES = listOf(
            LANG_IT, LANG_EN, LANG_DE, LANG_FR, LANG_ES, LANG_PT, LANG_NL, LANG_PL,
            LANG_SV, LANG_DA, LANG_FI, LANG_EL, LANG_CS, LANG_HU, LANG_RO, LANG_BG,
            LANG_SK, LANG_HR, LANG_LT, LANG_SL, LANG_LV, LANG_ET, LANG_GA, LANG_MT
        )
    }

    init {
        checkFirstStartup()
    }

    private fun checkFirstStartup() {
        if (prefs.getBoolean(KEY_FIRST_STARTUP, true)) {
            val deviceLanguage = Locale.getDefault().language
            val initialLanguage = if (SUPPORTED_LANGUAGES.contains(deviceLanguage)) {
                deviceLanguage
            } else {
                LANG_EN
            }
            
            prefs.edit()
                .putString(KEY_LANGUAGE, initialLanguage)
                .putBoolean(KEY_FIRST_STARTUP, false)
                .apply()
        }
    }

    fun hasSeenTutorial(): Boolean {
        return prefs.getBoolean(KEY_HAS_SEEN_TUTORIAL, false)
    }

    fun setTutorialSeen() {
        prefs.edit().putBoolean(KEY_HAS_SEEN_TUTORIAL, true).apply()
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
        return prefs.getString(KEY_LANGUAGE, LANG_EN) ?: LANG_EN
    }

    fun saveLanguage(lang: String) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
    }
    
    fun isSoundEnabled(): Boolean {
        return prefs.getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun saveSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun getDifficultyLevel(): DifficultyLevel {
        val name = prefs.getString(KEY_DIFFICULTY_LEVEL, DifficultyLevel.MEDIUM.name)
        return try {
            DifficultyLevel.valueOf(name!!)
        } catch (e: Exception) {
            DifficultyLevel.MEDIUM
        }
    }

    fun saveDifficultyLevel(level: DifficultyLevel) {
        prefs.edit().putString(KEY_DIFFICULTY_LEVEL, level.name).apply()
    }
}
