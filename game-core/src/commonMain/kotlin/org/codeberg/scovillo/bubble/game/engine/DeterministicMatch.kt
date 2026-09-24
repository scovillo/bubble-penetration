package org.codeberg.scovillo.bubble.game.engine

import org.codeberg.scovillo.bubble.game.Boundaries
import org.codeberg.scovillo.bubble.game.GameActionEvent
import org.codeberg.scovillo.bubble.game.MatchState

/** Headless convenience API around the actual [DeterministicMatchEngine]. */
class DeterministicMatch(
    private val seed: String,
    private val version: Int = MatchEngineConfig.CURRENT_REPLAY_VERSION,
) {
    suspend fun replay(input: MatchInput): MatchResult {
        require(input.viewportAspectRatio > 0) { "invalid viewport aspect ratio" }
        val boundaries = Boundaries().apply {
            update(
                if (input.viewportAspectRatio > 1) 10f else 10f / input.viewportAspectRatio.toFloat(),
                input.viewportAspectRatio.toFloat()
            )
        }
        val config = MatchEngineConfig(seed, version)
        val state = MatchState(timerValueMs = config.initialTimerSeconds)
        val engine = DeterministicMatchEngine(boundaries, config, state)
        input.events.forEachIndexed { index, event ->
            require(event.timestampMs % MATCH_SIMULATION_STEP_MS == 0L) {
                "event timestamp is not aligned to the simulation step"
            }
            require(event.timestampMs >= engine.durationMs) { "events are not ordered" }
            advanceTo(engine, event.timestampMs)
            requireResolvedEvent(engine, input.events, index - 1)
            require(engine.submitTap(event.x, event.y)) {
                "tap #${index + 1} for ${event.objectId} at ${event.timestampMs}ms does not hit a valid target"
            }
        }
        while (!engine.isGameOver) engine.advance(MATCH_SIMULATION_STEP_SECONDS) { }
        input.events.indices.forEach { requireResolvedEvent(engine, input.events, it) }
        return MatchResult(engine.score, engine.durationMs)
    }

    private fun requireResolvedEvent(
        engine: DeterministicMatchEngine,
        events: List<GameActionEvent>,
        index: Int
    ) {
        if (index < 0) return
        require(engine.log.getOrNull(index)?.objectId == events[index].objectId) {
            "tap does not match declared object"
        }
    }

    /**
     * A replay only records inputs, whereas the game creates objects every frame.
     * Advance in bounded steps so objects are generated at their scheduled time even
     * when two recorded taps are seconds apart.
     */
    private suspend fun advanceTo(engine: DeterministicMatchEngine, timestampMs: Long) {
        while (engine.durationMs < timestampMs) {
            require(!engine.isGameOver) { "event occurs after game over" }
            engine.advance(MATCH_SIMULATION_STEP_SECONDS) { }
        }
    }

}

data class MatchInput(val events: List<GameActionEvent>, val viewportAspectRatio: Double)
data class MatchResult(val score: Int, val finishedAtMs: Long)
