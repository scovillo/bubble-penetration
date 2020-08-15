package de.spicysources.bubblepenetration.animation

import android.widget.TextView

class BlinkAnimation(private val textView: TextView, private val minAlphaLimit: Float = 0.25f) {

    private var blinkStep = 0.035f

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