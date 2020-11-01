package de.spicysources.bubblepenetration.logic

import java.lang.Math.random
import kotlin.math.log

class GameSpeed {

    fun getBubbleSpeedFor(score: Int): Float {
        val speed = getScaledSpeedFor(score)
        return speed * 0.75f + random().toFloat() * speed * 0.25f
    }

    fun getStarSpeedFor(score: Int): Float {
        val speed = getScaledSpeedFor(score)
        return speed * 0.85f + random().toFloat() * speed * 0.15f
    }

    private fun getScaledSpeedFor(score: Int): Float {
        return 1 + 6 * log(0.00175f * score + 1, 2f)
    }

}