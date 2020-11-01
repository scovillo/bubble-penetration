package de.spicysources.bubblepenetration.logic

import kotlin.math.log

class Time {

    fun getBubbleTimeFor(speed: Float): Float {
        return 1.25f + -0.25f * log(0.75f * speed + 1, 2f)
    }

    fun getBubblePunishmentTimeFor(speed: Float): Float {
        return getBubbleTimeFor(speed) * 1.5f
    }

    fun getStarTimeFor(speed: Float): Float {
        return getBubbleTimeFor(speed) * 2
    }

}