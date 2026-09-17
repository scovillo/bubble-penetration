package org.codeberg.scovillo.bubble.game.engine

import org.codeberg.scovillo.bubble.game.Boundaries
import org.codeberg.scovillo.bubble.game.GameActionEvent
import org.codeberg.scovillo.bubble.game.MatchState

/** Headless convenience API around the actual [DeterministicMatchEngine]. */
class DeterministicMatch(private val seed: String) {
    suspend fun replay(input: MatchInput): MatchResult {
        require(input.viewportAspectRatio > 0) { "invalid viewport aspect ratio" }
        val boundaries = Boundaries().apply {
            update(if (input.viewportAspectRatio > 1) 10f else 10f / input.viewportAspectRatio.toFloat(), input.viewportAspectRatio.toFloat())
        }
        val state = MatchState(timerValueMs = INITIAL_TIMER_SECONDS)
        val engine = DeterministicMatchEngine(boundaries, MatchEngineConfig(seed), state)
        input.events.forEach { event ->
            require(event.timestampMs >= engine.durationMs) { "events are not ordered" }
            advanceTo(engine, event.timestampMs)
            val resolvedActions = engine.log.size
            require(engine.submitTap(event.x, event.y)) { "tap does not hit a valid target" }
            engine.advance(0f) { }
            require(engine.log.getOrNull(resolvedActions)?.objectId == event.objectId) {
                "tap does not match declared object"
            }
        }
        engine.advance(engine.timerSeconds) { }
        return MatchResult(engine.score, engine.durationMs)
    }

    /**
     * A replay only records inputs, whereas the game creates objects every frame.
     * Advance in bounded steps so objects are generated at their scheduled time even
     * when two recorded taps are seconds apart.
     */
    private suspend fun advanceTo(engine: DeterministicMatchEngine, timestampMs: Long) {
        while (engine.durationMs < timestampMs) {
            require(!engine.isGameOver) { "event occurs after game over" }
            val stepMs = minOf(REPLAY_TICK_MS, timestampMs - engine.durationMs)
            engine.advance(stepMs / 1_000f) { }
        }
    }

    private companion object {
        const val INITIAL_TIMER_SECONDS = 25f
        const val REPLAY_TICK_MS = 50L
    }
}

data class MatchInput(val events: List<GameActionEvent>, val viewportAspectRatio: Double)
data class MatchResult(val score: Int, val finishedAtMs: Long)
