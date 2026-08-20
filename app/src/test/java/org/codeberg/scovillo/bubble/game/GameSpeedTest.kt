package org.codeberg.scovillo.bubble.game

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.log

class GameSpeedTest {

    private val sut = GameSpeed()

    @Test
    fun bubbleSpeed_isAlwaysWithinExpectedRange() {
        val scores = listOf(0, 10, 100, 1_000, 10_000)

        for (score in scores) {
            val scaled = scaledSpeed(score)
            repeat(2_000) {
                val speed = sut.getBubbleSpeedFor(score)
                assertTrue(speed >= scaled * 0.75f, "Bubble speed below lower bound for score=$score")
                assertTrue(speed <= scaled, "Bubble speed above upper bound for score=$score")
            }
        }
    }

    @Test
    fun starSpeed_isAlwaysWithinExpectedRange() {
        val scores = listOf(0, 10, 100, 1_000, 10_000)

        for (score in scores) {
            val scaled = scaledSpeed(score)
            repeat(2_000) {
                val speed = sut.getStarSpeedFor(score)
                assertTrue(speed >= scaled * 0.85f, "Star speed below lower bound for score=$score")
                assertTrue(speed <= scaled, "Star speed above upper bound for score=$score")
            }
        }
    }

    @Test
    fun bubbleSpeed_averageFactorIsCloseToExpected() {
        val score = 2_500
        val scaled = scaledSpeed(score)
        val samples = 20_000

        var sum = 0.0
        repeat(samples) {
            sum += sut.getBubbleSpeedFor(score).toDouble()
        }
        val mean = (sum / samples).toFloat()
        val meanFactor = mean / scaled

        assertTrue(meanFactor >= 0.86f, "Mean factor too low: $meanFactor")
        assertTrue(meanFactor <= 0.89f, "Mean factor too high: $meanFactor")
    }

    @Test
    fun starSpeed_averageFactorIsCloseToExpected() {
        val score = 2_500
        val scaled = scaledSpeed(score)
        val samples = 20_000

        var sum = 0.0
        repeat(samples) {
            sum += sut.getStarSpeedFor(score).toDouble()
        }
        val mean = (sum / samples).toFloat()
        val meanFactor = mean / scaled

        assertTrue(meanFactor >= 0.915f, "Mean factor too low: $meanFactor")
        assertTrue(meanFactor <= 0.935f, "Mean factor too high: $meanFactor")
    }

    private fun scaledSpeed(score: Int): Float {
        return 1f + 6f * log(0.00125f * score + 1f, 2f)
    }
}
