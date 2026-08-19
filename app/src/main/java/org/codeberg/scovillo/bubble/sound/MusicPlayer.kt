package org.codeberg.scovillo.bubble.sound

import android.media.MediaPlayer
import org.codeberg.scovillo.bubble.MainActivity
import org.codeberg.scovillo.bubble.R

class MusicPlayer(private val mainActivity: MainActivity) {

    private lateinit var musicPlayer: MediaPlayer

    var isMuted = false
        set(value) {
            field = value
            if (value) {
                pause()
            } else {
                start()
            }
        }

    fun init() {
        musicPlayer = MediaPlayer.create(mainActivity, R.raw.music)
        musicPlayer.isLooping = true
        musicPlayer.setVolume(0.3f, 0.3f)
    }

    fun start() {
        if (!musicPlayer.isPlaying && !isMuted) {
            musicPlayer.start()
        }
    }

    fun pause() {
        if (musicPlayer.isPlaying) {
            musicPlayer.pause()
        }
    }

}