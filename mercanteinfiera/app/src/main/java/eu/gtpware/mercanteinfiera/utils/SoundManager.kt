package eu.gtpware.mercanteinfiera.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import eu.gtpware.mercanteinfiera.R

object SoundManager {
    private var soundPool: SoundPool? = null
    private var clickSoundId: Int = -1
    private var isLoaded = false
    private var isEnabled = true

    private var mediaPlayer: MediaPlayer? = null
    private var isMusicEnabled = true

    fun init(context: Context, enabled: Boolean = true, musicEnabled: Boolean = true) {
        isEnabled = enabled
        isMusicEnabled = musicEnabled
        
        if (soundPool == null) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            
            soundPool = SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build()

            soundPool?.let { pool ->
                clickSoundId = pool.load(context, R.raw.freesoundeffects_button_click_289742, 1)
                pool.setOnLoadCompleteListener { _, _, status ->
                    if (status == 0) {
                        isLoaded = true
                    }
                }
            }
        }

        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context, R.raw.ensnared_wonderland_1555471)
            mediaPlayer?.isLooping = true
            if (isMusicEnabled) {
                mediaPlayer?.start()
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun setMusicEnabled(enabled: Boolean) {
        isMusicEnabled = enabled
        if (isMusicEnabled) {
            if (mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
            }
        } else {
            mediaPlayer?.pause()
        }
    }

    fun pauseMusic() {
        mediaPlayer?.pause()
    }

    fun resumeMusic() {
        if (isMusicEnabled && mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start()
        }
    }

    fun playClickSound() {
        if (isEnabled && isLoaded) {
            soundPool?.play(clickSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        isLoaded = false
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
