package de.spicysources.bubblepenetration.sound

import android.media.MediaPlayer
import de.spicysources.bubblepenetration.MainActivity
import de.spicysources.bubblepenetration.R

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