package org.codeberg.scovillo.bubble.game

import kotlin.math.log
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimeTest {
    private val sut = Time()

    @Test
    fun bubbleTimeAtZeroSpeedIsExpected() {
        assertEquals(1.25f, sut.getBubbleTimeFor(0f), 1e-6f)
    }

    @Test
    fun derivedTimesKeepTheirFactors() {
        val bubble = sut.getBubbleTimeFor(7.5f)

        assertEquals(bubble * 1.5f, sut.getBubblePunishmentTimeFor(7.5f), 1e-6f)
        assertEquals(bubble * 2f, sut.getStarTimeFor(7.5f), 1e-6f)
    }

    @Test
    fun matchesThePublishedFormula() {
        val speed = 7.5f
        val expected = 1.25f - 0.25f * log(0.75f * speed + 1f, 2f)

        assertEquals(expected, sut.getBubbleTimeFor(speed), 1e-6f)
        assertTrue(sut.getBubbleTimeFor(3f) < sut.getBubbleTimeFor(1f))
    }
}
