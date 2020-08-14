package de.spicysources.bubblepenetration.util

import android.content.Context
import android.media.MediaPlayer
import de.spicysources.bubblepenetration.R
import java.util.*

class EffectTask(private val context: Context, muted: Boolean) : Thread() {

    private var muted = false
    private var ingame = true
    private val sounds: ArrayList<Int>
    private val effectPlayer: HashMap<Int, MediaPlayer>

    init {
        this.muted = muted
        sounds = ArrayList()
        effectPlayer = HashMap()
        effectPlayer[R.raw.blubb] = MediaPlayer.create(context, R.raw.blubb)
        effectPlayer[R.raw.fart] = MediaPlayer.create(context, R.raw.fart)
        effectPlayer[R.raw.star] = MediaPlayer.create(context, R.raw.star)
        effectPlayer[R.raw.alarm] = MediaPlayer.create(context, R.raw.alarm)
    }

    override fun run() {
        while (ingame) {
            if (sounds.isNotEmpty() and !muted) {
                if (effectPlayer[sounds[0]] == null) effectPlayer[sounds[0]] = MediaPlayer.create(context, sounds[0])
                effectPlayer[sounds[0]]!!.start()
                sounds.removeAt(0)
            }
        }
    }

    fun playSound(index: Int) {
        sounds.add(index)
    }

    fun setIngame(ingame: Boolean) {
        this.ingame = ingame
    }

}