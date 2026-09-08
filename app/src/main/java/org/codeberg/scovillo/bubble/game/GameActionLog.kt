package org.codeberg.scovillo.bubble.game

import android.os.SystemClock

enum class GameActionType(val wireValue: String) {
    BUBBLE_MATCH("bubble_match"),
    BUBBLE_MISMATCH("bubble_mismatch"),
    STAR("star"),
}

data class GameActionEvent(val type: GameActionType, val timestampMs: Long)

// Records what the server needs to recompute the score itself instead of trusting the client.
class GameActionLog {

    private val startElapsedRealtime = SystemClock.elapsedRealtime()
    private val events = mutableListOf<GameActionEvent>()

    @Synchronized
    fun record(type: GameActionType) {
        events.add(GameActionEvent(type, SystemClock.elapsedRealtime() - startElapsedRealtime))
    }

    @Synchronized
    fun snapshot(): List<GameActionEvent> = events.toList()
}
