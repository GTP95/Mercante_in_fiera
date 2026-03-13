package eu.gtpware.mercanteinfiera.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import eu.gtpware.mercanteinfiera.R

object SoundManager {
    private var soundPool: SoundPool? = null
    private var clickSoundId: Int = -1
    private var isLoaded = false

    fun init(context: Context) {
        if (soundPool != null) return

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool?.let { pool ->
            // Now that the file is renamed, we can use the R.raw reference directly
            clickSoundId = pool.load(context, R.raw.freesoundeffects_button_click_289742, 1)
            pool.setOnLoadCompleteListener { _, _, status ->
                if (status == 0) {
                    isLoaded = true
                }
            }
        }
    }

    fun playClickSound() {
        if (isLoaded) {
            soundPool?.play(clickSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        isLoaded = false
    }
}
