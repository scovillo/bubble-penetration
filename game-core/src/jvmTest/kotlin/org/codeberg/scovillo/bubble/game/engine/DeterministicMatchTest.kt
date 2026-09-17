package org.codeberg.scovillo.bubble.game.engine

import org.codeberg.scovillo.bubble.game.Boundaries
import org.codeberg.scovillo.bubble.game.GameActionEvent
import org.codeberg.scovillo.bubble.game.MatchState
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeterministicMatchTest {
    private val seed = "00".repeat(32)

    @Test
    fun acceptsAHitForTheGeneratedObject() {
        val event = generatedFirstObjectHit()

        val result = runSuspend {
            DeterministicMatch(seed).replay(MatchInput(listOf(event), viewportAspectRatio = 1.0))
        }

        assertEquals(1, result.score)
    }

    @Test
    fun rejectsAHitWhoseDeclaredObjectDiffersFromTheActualTarget() {
        val event = generatedFirstObjectHit().copy(objectId = "o_0_deadbeef")

        assertFailsWith<IllegalArgumentException> {
            runSuspend {
                DeterministicMatch(seed).replay(
                    MatchInput(
                        listOf(event),
                        viewportAspectRatio = 1.0
                    )
                )
            }
        }
    }

    @Test
    fun recordsTheOriginalTapInputForReplay() {
        val boundaries = Boundaries().apply { update(desiredHeight = 10f, aspectRatio = 1f) }
        val engine = DeterministicMatchEngine(
            boundaries,
            MatchEngineConfig(seed),
            MatchState(timerValueMs = 25f),
        )
        runSuspend { repeat(8) { engine.advance(.05f) { } } }
        val metadata = requireNotNull(engine.gameObjects.first().replayMetadata)
        val (x, y) = metadata.positionAt(400)

        assertEquals(true, engine.submitTap(x, y))
        runSuspend { engine.advance(.016f) { } }

        val event = engine.log.single()
        val (expectedX, expectedY) = metadata.positionAt(400)
        assertEquals(400, event.timestampMs)
        assertEquals(engine.roundCoordinate(expectedX), event.x)
        assertEquals(engine.roundCoordinate(expectedY), event.y)
    }

    @Test
    fun replaysActionsCapturedAcrossRenderFrames() {
        val boundaries = Boundaries().apply { update(desiredHeight = 10f, aspectRatio = 1f) }
        val engine = DeterministicMatchEngine(
            boundaries,
            MatchEngineConfig(seed),
            MatchState(timerValueMs = 25f),
        )

        repeat(500) {
            runSuspend { engine.advance(.016f) { } }
            engine.gameObjects.firstOrNull()?.replayMetadata?.let { metadata ->
                val (x, y) = metadata.positionAt(engine.durationMs)
                engine.submitTap(x, y)
            }
        }

        val result = runSuspend {
            DeterministicMatch(seed).replay(MatchInput(engine.log, viewportAspectRatio = 1.0))
        }

        assertEquals(engine.score, result.score)
    }

    private fun generatedFirstObjectHit(): GameActionEvent {
        val boundaries = Boundaries().apply { update(desiredHeight = 10f, aspectRatio = 1f) }
        val engine = DeterministicMatchEngine(
            boundaries,
            MatchEngineConfig(seed),
            MatchState(timerValueMs = 25f),
        )
        runSuspend { repeat(8) { engine.advance(.05f) { } } }
        val metadata = requireNotNull(engine.gameObjects.first().replayMetadata)
        val (x, y) = metadata.positionAt(400)
        return GameActionEvent(metadata.objectId, 400, x, y)
    }

    private fun <T> runSuspend(block: suspend () -> T): T {
        var outcome: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<T>) {
                outcome = result
            }
        })
        return requireNotNull(outcome).getOrThrow()
    }
}
