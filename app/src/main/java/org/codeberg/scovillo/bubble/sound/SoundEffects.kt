package org.codeberg.scovillo.bubble.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import org.codeberg.scovillo.bubble.R

class SoundEffects(context: Context) {

    @Volatile
    var isMuted = false

    private val lock = Any()
    private var released = false
    private var warmUpStreamId = 0
    private val loadedSounds = mutableSetOf<Int>()
    private val soundPool = createSoundPool()
    private val soundIds = mutableMapOf<Int, Int>()

    init {
        soundPool.setOnLoadCompleteListener { _, soundId, status ->
            if (status == SUCCESS) {
                synchronized(lock) {
                    loadedSounds.add(soundId)
                    keepAudioOutputActive(soundId)
                }
            }
        }
        EFFECT_RESOURCES.forEach { resourceId ->
            soundIds[resourceId] = soundPool.load(context, resourceId, 1)
        }
    }

    fun playSound(resourceId: Int) {
        if (isMuted) return

        synchronized(lock) {
            val soundId = soundIds[resourceId] ?: return
            if (released || soundId !in loadedSounds) return
            soundPool.play(soundId, VOLUME, VOLUME, PRIORITY, NO_LOOP, PLAYBACK_RATE)
        }
    }

    fun release() {
        synchronized(lock) {
            if (released) return
            released = true
            loadedSounds.clear()
            soundPool.release()
        }
    }

    /** Keeps SoundPool's output track ready before gameplay begins. */
    private fun keepAudioOutputActive(soundId: Int) {
        if (released || warmUpStreamId != 0) return
        warmUpStreamId = soundPool.play(
            soundId,
            SILENT_VOLUME,
            SILENT_VOLUME,
            PRIORITY,
            LOOP_FOREVER,
            PLAYBACK_RATE
        )
    }

    private fun createSoundPool(): SoundPool {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val attributes = AudioAttributes.Builder()
                // Match the background music so both streams use Android's media mixer.
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            return SoundPool.Builder()
                .setMaxStreams(MAX_SIMULTANEOUS_SOUNDS)
                .setAudioAttributes(attributes)
                .build()
        }

        @Suppress("DEPRECATION")
        return SoundPool(MAX_SIMULTANEOUS_SOUNDS, AudioManager.STREAM_MUSIC, 0)
    }

    private companion object {
        const val MAX_SIMULTANEOUS_SOUNDS = 9
        const val SUCCESS = 0
        const val VOLUME = 1.0f
        const val SILENT_VOLUME = 0.0f
        const val PRIORITY = 1
        const val NO_LOOP = 0
        const val LOOP_FOREVER = -1
        const val PLAYBACK_RATE = 1.0f

        val EFFECT_RESOURCES = intArrayOf(
            R.raw.blubb,
            R.raw.fart,
            R.raw.star,
            R.raw.alarm,
            R.raw.combo,
            R.raw.mega,
            R.raw.wow,
            R.raw.yeah
        )
    }

}
