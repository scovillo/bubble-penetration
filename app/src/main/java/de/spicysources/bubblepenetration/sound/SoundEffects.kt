package de.spicysources.bubblepenetration.sound

import android.content.Context
import android.media.MediaPlayer
import de.spicysources.bubblepenetration.R
import java.util.concurrent.ArrayBlockingQueue

class SoundEffects(private val context: Context) : Runnable {

    var isMuted = false
    var ingame = true

    private val soundsToPlay = ArrayBlockingQueue<Int>(100)
    private val effectPlayer = HashMap<Int, MediaPlayer>()

    init {
        effectPlayer[R.raw.blubb] = MediaPlayer.create(context, R.raw.blubb)
        effectPlayer[R.raw.fart] = MediaPlayer.create(context, R.raw.fart)
        effectPlayer[R.raw.star] = MediaPlayer.create(context, R.raw.star)
        effectPlayer[R.raw.alarm] = MediaPlayer.create(context, R.raw.alarm)
    }

    override fun run() {
        soundsToPlay.clear()
        while (ingame) {
            val soundIndex = soundsToPlay.take()
            if (!isMuted) {
                if (effectPlayer[soundIndex] == null) {
                    effectPlayer[soundIndex] = MediaPlayer.create(context, soundIndex)
                }
                effectPlayer[soundIndex]!!.start()
            }
        }
    }

    fun playSound(index: Int) {
        soundsToPlay.put(index)
    }

}