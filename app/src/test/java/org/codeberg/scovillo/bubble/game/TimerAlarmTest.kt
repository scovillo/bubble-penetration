package org.codeberg.scovillo.bubble.game

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TimerAlarmTest {
    @Test
    fun triggersOnlyWhenCrossingBelowThreshold() {
        val alarm = TimerAlarm()

        assertFalse(alarm.shouldTrigger(10.0f, 0L))
        assertTrue(alarm.shouldTrigger(9.9f, 1L))
        assertFalse(alarm.shouldTrigger(9.0f, 20_000L))
    }

    @Test
    fun suppressesRepeatedCrossingsWithinTenSeconds() {
        val alarm = TimerAlarm()

        assertTrue(alarm.shouldTrigger(9.9f, 1_000L))
        assertFalse(alarm.shouldTrigger(10.1f, 2_000L))
        assertFalse(alarm.shouldTrigger(9.9f, 2_001L))
        assertFalse(alarm.shouldTrigger(10.1f, 10_999L))
        assertFalse(alarm.shouldTrigger(9.9f, 11_000L))
    }

    @Test
    fun triggersAgainOnNewCrossingAfterTenSeconds() {
        val alarm = TimerAlarm()

        assertTrue(alarm.shouldTrigger(9.9f, 1_000L))
        assertFalse(alarm.shouldTrigger(10.1f, 11_000L))
        assertFalse(alarm.shouldTrigger(9.9f, 11_000L))
        assertFalse(alarm.shouldTrigger(10.1f, 11_001L))
        assertTrue(alarm.shouldTrigger(9.9f, 11_001L))
    }

    @Test
    fun doesNotTriggerLateIfCrossingWasSuppressed() {
        val alarm = TimerAlarm()

        assertTrue(alarm.shouldTrigger(9.9f, 1_000L))
        assertFalse(alarm.shouldTrigger(10.1f, 2_000L))
        assertFalse(alarm.shouldTrigger(9.9f, 2_001L))
        assertFalse(alarm.shouldTrigger(9.0f, 20_000L))
    }
}
