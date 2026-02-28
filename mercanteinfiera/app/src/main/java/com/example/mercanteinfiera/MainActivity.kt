package com.example.mercanteinfiera

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mercanteinfiera.ui.theme.MercanteInFieraTheme
import com.example.mercanteinfiera.viewmodel.GameViewModel

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: GameViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val language by viewModel.language.collectAsState()
            
            LaunchedEffect(language) {
                val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(language)
                AppCompatDelegate.setApplicationLocales(appLocale)
            }

            val isDarkTheme = when (themeMode) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }

            MercanteInFieraTheme(darkTheme = isDarkTheme, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameScreen(viewModel = viewModel)
                }
            }
        }
    }
}