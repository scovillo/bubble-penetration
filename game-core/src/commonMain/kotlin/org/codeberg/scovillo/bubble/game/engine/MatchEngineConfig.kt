package org.codeberg.scovillo.bubble.game.engine

/** Versioned constants shared by every deterministic match implementation. */
class MatchEngineConfig(val seed: String, val replayVersion: Int = 1) {
    val maxObjectsOnField = 20
    val spawnIntervalMs = 250L
    val baseTravelDurationMs = 9_000.0
    val targetChangeBaseMs = 6_000L
    val minBubbleScale = 0.75f
    val bubbleScaleRange = 0.15f
    val starScale = 0.85f
    val maxSpawnDelayMs = 100L
    val minSpawnDistance = 1.5f
    val minReactionMs = 100L
    val minEventGapMs = 30L
}
