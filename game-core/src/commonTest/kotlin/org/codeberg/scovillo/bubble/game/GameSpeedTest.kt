package org.codeberg.scovillo.bubble.game

import kotlin.math.log
import kotlin.test.Test
import kotlin.test.assertTrue

class GameSpeedTest {
    private val sut = GameSpeed()

    @Test
    fun bubbleSpeedIsWithinExpectedRange() {
        listOf(0, 10, 100, 1_000, 10_000).forEach { score ->
            val scaled = scaledSpeed(score)
            repeat(2_000) {
                val speed = sut.getBubbleSpeedFor(score)
                assertTrue(speed >= scaled * .75f && speed <= scaled)
            }
        }
    }

    @Test
    fun starSpeedIsWithinExpectedRange() {
        listOf(0, 10, 100, 1_000, 10_000).forEach { score ->
            val scaled = scaledSpeed(score)
            repeat(2_000) {
                val speed = sut.getStarSpeedFor(score)
                assertTrue(speed >= scaled * .85f && speed <= scaled)
            }
        }
    }

    private fun scaledSpeed(score: Int): Float = 1f + 6f * log(.00125f * score + 1f, 2f)
}
