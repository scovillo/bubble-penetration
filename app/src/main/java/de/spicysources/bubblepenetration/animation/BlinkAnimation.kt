package de.spicysources.bubblepenetration.animation

import android.widget.TextView

class BlinkAnimation(private val textView: TextView, private var blinkStep: Float = 0.035f, private val minAlphaLimit: Float = 0.25f) {

    fun update() {
        if (textView.alpha > 1 || textView.alpha < minAlphaLimit) {
            blinkStep *= -1f
        }
        textView.alpha = textView.alpha + blinkStep * 2
    }

    fun stop() {
        textView.alpha = 1.0f
    }

}