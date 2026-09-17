package org.codeberg.scovillo.bubble.game

import kotlin.math.log

/** Timing rules for collecting bubbles and stars. */
class Time {
    fun getBubbleTimeFor(speed: Float): Float =
        1.25f - 0.25f * log(0.75f * speed + 1, 2f)

    fun getBubblePunishmentTimeFor(speed: Float): Float = getBubbleTimeFor(speed) * 1.5f

    fun getStarTimeFor(speed: Float): Float = getBubbleTimeFor(speed) * 2
}
