package de.spicysources.bubblepenetration.logic

import android.content.Context.VIBRATOR_SERVICE
import android.graphics.Typeface
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import de.spicysources.bubblepenetration.MainActivity
import de.spicysources.bubblepenetration.R
import de.spicysources.bubblepenetration.sound.SoundEffects
import java.lang.System.currentTimeMillis
import kotlin.math.pow

class Combo(private val mainActivity: MainActivity, private val effectPlayer: SoundEffects) {

    val multiplikator
        get() = run {
            val factor = counter / comboCollectFactor
            if (factor > 0) {
                val multiplikator = 2.0.pow(factor.toDouble()).toInt()
                if (multiplikator > 16) 16 else multiplikator
            } else 1
        }

    val isActive
        get() = multiplikator > 1

    private var counter = 0

    private val comboCollectFactor = 6

    private val duration = 2500

    private var lastBubble = currentTimeMillis()

    private var textView = mainActivity.findViewById<View>(R.id.Combo) as TextView

    private val textAnimation: Animation = AlphaAnimation(0.35f, 1.0f)

    private val symbol = "●"

    private val vibrator = mainActivity.getSystemService(VIBRATOR_SERVICE) as Vibrator

    init {
        textView.typeface = Typeface.createFromAsset(mainActivity.assets, "fonts/PLUMP.ttf")
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
                comboProgressText = "Combo x$multiplikator "
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
        val oldMultiplikator = multiplikator
        counter++
        lastBubble = currentTimeMillis()
        if (multiplikator > oldMultiplikator && !effectPlayer.isMuted) {
            when (multiplikator) {
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
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(25, getAmplitude()))
            } else {
                vibrator.vibrate(25)
            }
        }
    }

    private fun getAmplitude(): Int {
        return when (multiplikator) {
            2 -> 75
            4 -> 125
            8 -> 175
            16 -> 255
            else -> 0
        }
    }

}