package de.spicysources.bubblepenetration.util

import android.content.Context
import android.media.MediaPlayer
import de.spicysources.bubblepenetration.R
import java.util.concurrent.ArrayBlockingQueue

class EffectTask(private val context: Context, muted: Boolean) : Thread() {

    private var muted = false
    private var ingame = true
    private val soundsToPlay = ArrayBlockingQueue<Int>(100)
    private val effectPlayer = HashMap<Int, MediaPlayer>()

    init {
        this.muted = muted
        effectPlayer[R.raw.blubb] = MediaPlayer.create(context, R.raw.blubb)
        effectPlayer[R.raw.fart] = MediaPlayer.create(context, R.raw.fart)
        effectPlayer[R.raw.star] = MediaPlayer.create(context, R.raw.star)
        effectPlayer[R.raw.alarm] = MediaPlayer.create(context, R.raw.alarm)
    }

    override fun run() {
        soundsToPlay.clear()
        while (ingame) {
            val soundIndex = soundsToPlay.take()
            if (!muted) {
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

    fun setIngame(ingame: Boolean) {
        this.ingame = ingame
    }

}