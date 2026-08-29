package org.codeberg.scovillo.bubble.game

class TimerAlarm(
    private val thresholdSeconds: Float = 10.0f,
    private val debounceMillis: Long = 10_000L,
) {
    private var wasBelowThreshold = false
    private var lastAlarmMillis: Long? = null

    fun shouldTrigger(timerSeconds: Float, nowMillis: Long): Boolean {
        val isBelowThreshold = timerSeconds < thresholdSeconds
        val crossedThreshold = isBelowThreshold && !wasBelowThreshold
        wasBelowThreshold = isBelowThreshold

        if (!crossedThreshold) return false

        val lastAlarm = lastAlarmMillis
        if (lastAlarm != null && nowMillis - lastAlarm <= debounceMillis) return false

        lastAlarmMillis = nowMillis
        return true
    }
}
