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
        var currentMs = 0L
        input.events.forEach { event ->
            require(event.timestampMs >= currentMs) { "events are not ordered" }
            engine.advance((event.timestampMs - currentMs) / 1_000f) { }
            require(engine.submitTap(event.x, event.y)) { "tap does not hit a valid target" }
            engine.advance(0f) { }
            currentMs = event.timestampMs
        }
        engine.advance(engine.timerSeconds) { }
        return MatchResult(engine.score, engine.durationMs)
    }

    private companion object { const val INITIAL_TIMER_SECONDS = 25f }
}

data class MatchInput(val events: List<GameActionEvent>, val viewportAspectRatio: Double)
data class MatchResult(val score: Int, val finishedAtMs: Long)
