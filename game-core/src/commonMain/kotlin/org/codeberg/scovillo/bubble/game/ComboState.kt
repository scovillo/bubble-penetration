package org.codeberg.scovillo.bubble.game

import kotlin.math.pow

/** Game-rule state for a streak of correctly collected bubbles. */
class ComboState(
    private val collectFactor: Int = 6,
    private val durationMs: Long = 2_500L,
) {
    private var counter = 0
    private var lastBubbleMs: Long? = null

    val multiplier: Int
        get() = (2.0.pow((counter / collectFactor).toDouble()).toInt()).coerceAtMost(16)
    val isActive: Boolean get() = multiplier > 1
    val progress: Int get() = counter

    fun expireAt(elapsedMs: Long): Boolean {
        if (lastBubbleMs?.let { elapsedMs > it + durationMs } == true) {
            reset()
            return true
        }
        return false
    }

    fun increment(elapsedMs: Long): Int {
        val previousMultiplier = multiplier
        counter++
        lastBubbleMs = elapsedMs
        return previousMultiplier
    }

    fun reset() {
        counter = 0
        lastBubbleMs = null
    }
}
