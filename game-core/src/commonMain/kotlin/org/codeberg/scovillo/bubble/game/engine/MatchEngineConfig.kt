package org.codeberg.scovillo.bubble.game.engine

/** Versioned constants shared by every deterministic match implementation. */
class MatchEngineConfig(
    val seed: String,
    val replayVersion: Int = CURRENT_REPLAY_VERSION,
) {
    /** Maximum number of generated objects kept active at once. */
    val maxObjectsOnField: Int

    /** Minimum delay between generating new objects. */
    val spawnIntervalMs: Long

    /** Travel duration used for newly generated objects. */
    val baseTravelDurationMs: Float

    /** Base interval at which the collectible target changes. */
    val targetChangeBaseMs: Long

    /** Minimum bubble scale in world units. */
    val minBubbleScale: Float

    /** Additional random scale range applied to bubbles. */
    val bubbleScaleRange: Float

    /** Scale of generated stars in world units. */
    val starScale: Float

    /** Maximum random delay added to object spawning. */
    val maxSpawnDelayMs: Long

    /** Minimum distance between simultaneously generated objects. */
    val minSpawnDistance: Float

    /** Minimum time a target must be visible before it can be tapped. */
    val minReactionMs: Long

    /** Minimum time between two replayable taps. */
    val minEventGapMs: Long

    init {
        when (replayVersion) {
            1 -> {
                maxObjectsOnField = 20
                spawnIntervalMs = 100L
                baseTravelDurationMs = 10_000.0f
                targetChangeBaseMs = 6_000L
                minBubbleScale = 0.75f
                bubbleScaleRange = 0.15f
                starScale = 0.85f
                maxSpawnDelayMs = 150L
                minSpawnDistance = 1.5f
                minReactionMs = 100L
                minEventGapMs = 30L
            }

            else -> error("Unsupported replay version: $replayVersion")
        }
    }

    /** Stable, seed-free representation used to compare client and server builds. */
    fun summary(): String =
        "replayVersion=$replayVersion, maxObjectsOnField=$maxObjectsOnField, " +
                "spawnIntervalMs=$spawnIntervalMs, baseTravelDurationMs=$baseTravelDurationMs, " +
                "targetChangeBaseMs=$targetChangeBaseMs, minBubbleScale=$minBubbleScale, " +
                "bubbleScaleRange=$bubbleScaleRange, starScale=$starScale, " +
                "maxSpawnDelayMs=$maxSpawnDelayMs, minSpawnDistance=$minSpawnDistance, " +
                "minReactionMs=$minReactionMs, minEventGapMs=$minEventGapMs"

    companion object {
        const val CURRENT_REPLAY_VERSION = 1
    }
}
