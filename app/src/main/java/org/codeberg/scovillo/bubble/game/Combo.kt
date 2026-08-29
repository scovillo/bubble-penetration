package org.codeberg.scovillo.bubble.game

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.codeberg.scovillo.bubble.MainActivity
import org.codeberg.scovillo.bubble.R
import org.codeberg.scovillo.bubble.sound.SoundEffects
import java.lang.System.currentTimeMillis
import kotlin.math.pow

class Combo(private val mainActivity: MainActivity, private val effectPlayer: SoundEffects) {

    val multiplier
        get() = run {
            val factor = counter / comboCollectFactor
            if (factor > 0) {
                val multiplier = 2.0.pow(factor.toDouble()).toInt()
                if (multiplier > 16) 16 else multiplier
            } else 1
        }

    val isActive
        get() = multiplier > 1

    private var counter = 0

    private val comboCollectFactor = 6

    private val duration = 2500

    private var lastBubble = currentTimeMillis()

    private var textView = mainActivity.findViewById<View>(R.id.Combo) as TextView

    private val textAnimation: Animation = AlphaAnimation(0.35f, 1.0f)

    private val symbol = "●"

    private val vibrator = checkNotNull(ContextCompat.getSystemService(mainActivity, Vibrator::class.java))

    init {
        textAnimation.duration = 300
        textAnimation.startOffset = 20
        textAnimation.repeatMode = Animation.REVERSE
        textAnimation.repeatCount = Animation.INFINITE
    }

    fun update() {
        if (currentTimeMillis() > lastBubble + duration) {
            reset()
        }
        mainActivity.runOnUiThread {
            var comboProgressText = ""

            if (isActive) {
                comboProgressText = "Combo x$multiplier "
                if (textView.animation == null) {
                    textView.startAnimation(textAnimation)
                }
            } else {
                textView.animation?.cancel()
            }
            for (i in 0 until counter % comboCollectFactor) {
                comboProgressText = "$comboProgressText$symbol"
            }
            textView.text = comboProgressText
        }
    }

    fun increment() {
        val oldMultiplier = multiplier
        counter++
        lastBubble = currentTimeMillis()
        if (multiplier > oldMultiplier && !effectPlayer.isMuted) {
            when (multiplier) {
                2 -> effectPlayer.playSound(R.raw.combo)
                4 -> effectPlayer.playSound(R.raw.wow)
                8 -> effectPlayer.playSound(R.raw.yeah)
                16 -> effectPlayer.playSound(R.raw.mega)
            }
        }
    }

    fun reset() {
        counter = 0
    }

    fun giveHapticFeedBack() {
        if (!mainActivity.settingsModel.isVibrationEnabled) return
        Log.d("Combo", "hasVibrator=${vibrator.hasVibrator()}")
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d("Combo", "Vibrate for sdk=${Build.VERSION.SDK_INT}")
                vibrator.vibrate(VibrationEffect.createOneShot(50, getAmplitude()))
            } else {
                Log.d("Combo", "Vibrate legacy")
                vibrateLegacy()
            }
        }
    }

    private fun getAmplitude(): Int {
        val value = when (multiplier) {
            2 -> 75
            4 -> 125
            8 -> 175
            16 -> 255
            else -> 0
        }
        Log.d("Combo", "Multiplier ${multiplier}=${value} Amplitude")
        return value
    }

    @Suppress("DEPRECATION")
    private fun vibrateLegacy() {
        vibrator.vibrate(25)
    }

}
