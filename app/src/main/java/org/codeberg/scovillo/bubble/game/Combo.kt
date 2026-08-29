package org.codeberg.scovillo.bubble.game

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
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

    private var pulsingMultiplier = 0
    private var unstoppableColorAnimator: ValueAnimator? = null
    @Volatile private var targetAccentColor: Int? = null
    private var lastAppliedTargetAccentColor: Int? = null

    private val symbol = "●"
    private val emptySymbol = "○"
    private var lastRenderedText = ""
    private var lastRenderedMultiplier = 0

    private val vibrator = checkNotNull(ContextCompat.getSystemService(mainActivity, Vibrator::class.java))

    fun update() {
        if (currentTimeMillis() > lastBubble + duration) {
            reset()
        }
        mainActivity.runOnUiThread {
            val hasComboProgress = counter > 0
            val comboProgressText = if (hasComboProgress) {
                val levelLabel = when (multiplier) {
                    1 -> mainActivity.getString(R.string.combo_build)
                    2 -> mainActivity.getString(R.string.combo)
                    4 -> mainActivity.getString(R.string.combo_hot_streak)
                    8 -> mainActivity.getString(R.string.combo_on_fire)
                    else -> mainActivity.getString(R.string.combo_mega)
                }
                val filledSteps = counter % comboCollectFactor
                "$levelLabel ×$multiplier  ${symbol.repeat(filledSteps)}${emptySymbol.repeat(comboCollectFactor - filledSteps)}"
            } else {
                ""
            }

            if (hasComboProgress) {
                textView.visibility = View.VISIBLE
                updatePulseFor(multiplier)
            } else {
                textView.animation?.cancel()
                pulsingMultiplier = 0
                stopUnstoppableColorCycle()
                textView.visibility = View.INVISIBLE
            }

            if (comboProgressText != lastRenderedText) {
                textView.text = comboProgressText
                lastRenderedText = comboProgressText
            }
            if (hasComboProgress && multiplier != lastRenderedMultiplier) {
                styleFor(multiplier)
                playLevelUpPop()
                lastRenderedMultiplier = multiplier
            } else if (hasComboProgress && multiplier < 16 && targetAccentColor != lastAppliedTargetAccentColor) {
                styleFor(multiplier)
            }
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

    /** Called by the renderer as soon as the collection target changes. */
    fun setTargetColor(color: FloatArray) {
        targetAccentColor = Color.rgb(
            (color[0] * 255).toInt(),
            (color[1] * 255).toInt(),
            (color[2] * 255).toInt(),
        )
    }

    fun reset() {
        counter = 0
    }

    private fun styleFor(multiplier: Int) {
        stopUnstoppableColorCycle()
        val (defaultAccent, fill) = when (multiplier) {
            1 -> Color.rgb(82, 220, 255) to Color.rgb(14, 53, 82)
            2 -> Color.rgb(82, 220, 255) to Color.rgb(14, 53, 82)
            4 -> Color.rgb(183, 109, 255) to Color.rgb(51, 24, 82)
            8 -> Color.rgb(255, 126, 62) to Color.rgb(85, 34, 28)
            else -> Color.rgb(255, 204, 74) to Color.rgb(89, 63, 20)
        }
        val accent = if (multiplier < 16) targetAccentColor ?: defaultAccent else defaultAccent
        applyPillStyle(accent, fill)
        if (multiplier < 16) lastAppliedTargetAccentColor = accent
        if (multiplier == 16) startUnstoppableColorCycle()
    }

    private fun applyPillStyle(accent: Int, fill: Int) {
        val density = mainActivity.resources.displayMetrics.density
        ViewCompat.setBackground(textView, GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 18f * density
            setColor(fill)
            setStroke((2f * density).toInt(), accent)
        })
        textView.setTextColor(Color.WHITE)
    }

    private fun updatePulseFor(multiplier: Int) {
        if (multiplier == pulsingMultiplier) return
        textView.clearAnimation()
        pulsingMultiplier = multiplier
        val duration = when (multiplier) {
            2 -> 900L
            4 -> 650L
            8 -> 440L
            16 -> 260L
            else -> return // BUILD COMBO remains deliberately calm.
        }
        textView.startAnimation(AlphaAnimation(0.18f, 1.0f).apply {
            this.duration = duration
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        })
    }

    private fun startUnstoppableColorCycle() {
        unstoppableColorAnimator = ValueAnimator.ofObject(
            ArgbEvaluator(),
            Color.rgb(255, 204, 74),
            Color.rgb(255, 84, 144),
            Color.rgb(123, 104, 255),
            Color.rgb(72, 232, 255),
            Color.rgb(255, 204, 74),
        ).apply {
            duration = 900L
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animator ->
                val accent = animator.animatedValue as Int
                applyPillStyle(accent, Color.rgb(55, 36, 71))
            }
            start()
        }
    }

    private fun stopUnstoppableColorCycle() {
        unstoppableColorAnimator?.cancel()
        unstoppableColorAnimator = null
    }

    private fun playLevelUpPop() {
        textView.scaleX = 1f
        textView.scaleY = 1f
        ViewCompat.animate(textView).scaleX(1.18f).scaleY(1.18f).setDuration(120).withEndAction {
            ViewCompat.animate(textView).scaleX(1f).scaleY(1f).setDuration(160).start()
        }.start()
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
