package org.codeberg.scovillo.bubble.game

class GameActionLog {
    private val events = mutableListOf<GameActionEvent>()

    @Synchronized
    fun record(objectId: String, timestampMs: Long, x: Double, y: Double) {
        events.add(GameActionEvent(objectId, timestampMs, x, y))
    }

    @Synchronized
    fun snapshot(): List<GameActionEvent> = events.toList()
}
