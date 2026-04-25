package eu.gtpware.mercanteinfiera

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import eu.gtpware.mercanteinfiera.ui.theme.MercanteInFieraTheme
import eu.gtpware.mercanteinfiera.utils.SoundManager
import eu.gtpware.mercanteinfiera.viewmodel.GameViewModel
import eu.gtpware.mercanteinfiera.viewmodel.MultiplayerViewModel

class MainActivity : AppCompatActivity() {
    private val gameViewModel: GameViewModel by viewModels()
    private val multiplayerViewModel: MultiplayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val settingsManager = eu.gtpware.mercanteinfiera.data.SettingsManager(this)
        SoundManager.init(this, settingsManager.isSoundEnabled(), settingsManager.isMusicEnabled())
        handleIntent(intent)

        setContent {
            val themeMode by gameViewModel.themeMode.collectAsState()
            val language by gameViewModel.language.collectAsState()
            
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
                    GameScreen(viewModel = gameViewModel, multiplayerViewModel = multiplayerViewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        SoundManager.resumeMusic()
    }

    override fun onPause() {
        super.onPause()
        SoundManager.pauseMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        SoundManager.release()
    }

    private fun handleIntent(intent: Intent) {
        val data = intent.data
        if (intent.action == Intent.ACTION_VIEW && data != null) {
            val pathSegments = data.pathSegments
            if (pathSegments.size >= 2 && pathSegments[0] == "join") {
                val code = pathSegments[1]
                val playerName = gameViewModel.playerName.value.ifBlank { "Player" }
                multiplayerViewModel.joinRoom(code, playerName)
            }
        }
    }
}