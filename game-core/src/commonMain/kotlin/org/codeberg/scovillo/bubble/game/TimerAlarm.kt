package org.codeberg.scovillo.bubble.game

class TimerAlarm(
    private val thresholdSeconds: Float = 10f,
    private val debounceMillis: Long = 10_000L,
) {
    private var wasBelowThreshold = false
    private var lastAlarmMillis: Long? = null

    fun shouldTrigger(timerSeconds: Float, nowMillis: Long): Boolean {
        val belowThreshold = timerSeconds < thresholdSeconds
        val crossedThreshold = belowThreshold && !wasBelowThreshold
        wasBelowThreshold = belowThreshold
        if (!crossedThreshold) return false
        if (lastAlarmMillis?.let { nowMillis - it <= debounceMillis } == true) return false
        lastAlarmMillis = nowMillis
        return true
    }
}
