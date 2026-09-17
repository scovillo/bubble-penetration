package org.codeberg.scovillo.bubble.game

/** A normalized player input recorded for deterministic replay validation. */
data class GameActionEvent(
    val objectId: String,
    val timestampMs: Long,
    val x: Double,
    val y: Double,
)
