package org.codeberg.scovillo.bubble.android.ui.hud

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.widget.TextView
import org.codeberg.scovillo.bubble.R
import org.codeberg.scovillo.bubble.game.engine.MatchSnapshot

class Combo(private val textView: TextView) {
    private val defaultColor = textView.currentTextColor
    private var pulseAnimator: ValueAnimator? = null
    private var resetAnimator: ValueAnimator? = null
    private var pulseMultiplier: Int? = null
    private var lastRenderedMultiplier = 1
    private var isHiding = false

    val isPulsing: Boolean
        get() = pulseAnimator != null

    fun soundResource(multiplier: Int): Int = when (multiplier) {
        2 -> R.raw.combo
        4 -> R.raw.wow
        8 -> R.raw.yeah
        else -> R.raw.mega
    }

    fun pulseDuration(multiplier: Int): Long = when (multiplier) {
        2 -> 1000L
        4 -> 650L
        8 -> 360L
        else -> 180L
    }

    fun multiplierIncreased(snapshot: MatchSnapshot): Boolean {
        val multiplierIncreased = snapshot.comboMultiplier > lastRenderedMultiplier
        lastRenderedMultiplier = snapshot.comboMultiplier
        return multiplierIncreased
    }

    fun render(snapshot: MatchSnapshot) {
        if (snapshot.comboProgress == 0) {
            if (!isHiding) {
                isHiding = true
                stopPulse {
                    textView.visibility = TextView.INVISIBLE
                    isHiding = false
                }
            }
            return
        }

        isHiding = false

        val filledSteps = snapshot.comboProgress % 6
        val label = when (snapshot.comboMultiplier) {
            1 -> textView.context.getString(R.string.combo_build)
            2 -> textView.context.getString(R.string.combo)
            4 -> textView.context.getString(R.string.combo_hot_streak)
            8 -> textView.context.getString(R.string.combo_on_fire)
            else -> textView.context.getString(R.string.combo_mega)
        }
        textView.text =
            "$label ×${snapshot.comboMultiplier}  ${"●".repeat(filledSteps)}${"○".repeat(6 - filledSteps)}"
        textView.visibility = TextView.VISIBLE

        if (snapshot.comboMultiplier <= 1) {
            stopPulse()
        } else if (pulseMultiplier != snapshot.comboMultiplier) {
            startPulse(snapshot.comboMultiplier)
        }
    }

    private fun startPulse(multiplier: Int) {
        val duration = pulseDuration(multiplier)
        resetAnimator?.let { animator ->
            resetAnimator = null
            animator.cancel()
        }
        pulseAnimator?.cancel()
        pulseAnimator = ValueAnimator.ofFloat(1f, 0.25f).apply {
            this.duration = duration
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                textView.alpha = progress
                if (multiplier >= 16) {
                    textView.setTextColor(
                        ArgbEvaluator().evaluate(
                            progress,
                            defaultColor,
                            Color.rgb(255, 70, 70)
                        ) as Int
                    )
                }
            }
            start()
        }
        pulseMultiplier = multiplier
    }

    private fun stopPulse(onComplete: (() -> Unit)? = null) {
        if (pulseAnimator == null && pulseMultiplier == null) {
            onComplete?.invoke()
            return
        }
        pulseAnimator?.cancel()
        pulseAnimator = null
        pulseMultiplier = null
        resetAnimator?.cancel()
        val startColor = textView.currentTextColor
        resetAnimator = ValueAnimator.ofFloat(textView.alpha, 1f).apply {
            duration = PULSE_RESET_DURATION_MS
            addUpdateListener { animator ->
                val progress = animator.animatedFraction
                textView.alpha = animator.animatedValue as Float
                textView.setTextColor(
                    ArgbEvaluator().evaluate(progress, startColor, defaultColor) as Int
                )
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (resetAnimator === animation) onComplete?.invoke()
                }
            })
            start()
        }
    }

    private companion object {
        const val PULSE_RESET_DURATION_MS = 180L
    }
}
