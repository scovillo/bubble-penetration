package org.codeberg.scovillo.bubble.game

class GameActionLog {
    private val events = mutableListOf<GameActionEvent>()

    fun record(objectId: String, timestampMs: Long, x: Double, y: Double) {
        events.add(GameActionEvent(objectId, timestampMs, x, y))
    }

    fun snapshot(): List<GameActionEvent> = events.toList()
}