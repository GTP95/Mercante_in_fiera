package eu.gtpware.mercanteinfiera.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun rememberClickable(onClick: () -> Unit): () -> Unit {
    return remember(onClick) {
        {
            SoundManager.playClickSound()
            onClick()
        }
    }
}
