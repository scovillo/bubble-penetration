package org.codeberg.scovillo.bubble.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.log

class TimeTest {

    private val sut = Time()

    @Test
    fun bubbleTime_atZeroSpeed_isExpected() {
        assertEquals(1.25f, sut.getBubbleTimeFor(0f), 1e-6f)
    }

    @Test
    fun bubblePunishmentTime_isAlwaysOnePointFiveTimesBubbleTime() {
        val speeds = listOf(0f, 0.5f, 1f, 3f, 7.5f, 15f)

        for (speed in speeds) {
            val bubble = sut.getBubbleTimeFor(speed)
            val punishment = sut.getBubblePunishmentTimeFor(speed)
            assertEquals(bubble * 1.5f, punishment, 1e-6f)
        }
    }

    @Test
    fun starTime_isAlwaysDoubleBubbleTime() {
        val speeds = listOf(0f, 0.5f, 1f, 3f, 7.5f, 15f)

        for (speed in speeds) {
            val bubble = sut.getBubbleTimeFor(speed)
            val star = sut.getStarTimeFor(speed)
            assertEquals(bubble * 2f, star, 1e-6f)
        }
    }

    @Test
    fun bubbleTime_decreasesWhenSpeedIncreases() {
        val t1 = sut.getBubbleTimeFor(1f)
        val t2 = sut.getBubbleTimeFor(3f)
        val t3 = sut.getBubbleTimeFor(10f)

        assertTrue(t2 < t1)
        assertTrue(t3 < t2)
    }

    @Test
    fun bubbleTime_matchesFormulaForKnownSpeed() {
        val speed = 7.5f
        val expected = 1.25f + -0.25f * log(0.75f * speed + 1f, 2f)

        assertEquals(expected, sut.getBubbleTimeFor(speed), 1e-6f)
    }
}
