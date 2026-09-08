package org.codeberg.scovillo.bubble.game

data class GameActionEvent(
    val objectId: String,
    val timestampMs: Long,
    val x: Double,
    val y: Double,
)

class GameActionLog {
    private val events = mutableListOf<GameActionEvent>()

    @Synchronized
    fun record(objectId: String, timestampMs: Long, x: Double, y: Double) {
        events.add(GameActionEvent(objectId, timestampMs, x, y))
    }

    @Synchronized
    fun snapshot(): List<GameActionEvent> = events.toList()
}
