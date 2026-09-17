package org.codeberg.scovillo.bubble.game

import kotlin.math.log
import kotlin.random.Random

/** Speed scaling shared by deterministic matches and randomized menu previews. */
class GameSpeed {

    fun getBubbleSpeedFor(score: Int): Float {
        val speed = getScaledSpeedFor(score)
        return speed * 0.75f + Random.nextFloat() * speed * 0.25f
    }

    fun getStarSpeedFor(score: Int): Float {
        val speed = getScaledSpeedFor(score)
        return speed * 0.85f + Random.nextFloat() * speed * 0.15f
    }

    private fun getScaledSpeedFor(score: Int): Float =
        1f + 6f * log(0.00125f * score + 1f, 2f)
}
