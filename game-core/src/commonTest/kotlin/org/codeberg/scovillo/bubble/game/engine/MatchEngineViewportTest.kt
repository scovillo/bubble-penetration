package org.codeberg.scovillo.bubble.game.engine

import org.codeberg.scovillo.bubble.game.Boundaries
import org.codeberg.scovillo.bubble.game.GameObject
import org.codeberg.scovillo.bubble.game.GameObjectColor
import org.codeberg.scovillo.bubble.game.GameObjectType
import org.codeberg.scovillo.bubble.game.MatchState
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals

class MatchEngineViewportTest {
    @Test
    fun keepsObjectEnteringFromOutsideUntilItsWholeBodyLeavesTheViewport() {
        val boundaries = Boundaries().apply { update(desiredHeight = 10f, aspectRatio = 1f) }
        val enteringObject = GameObject(
            type = GameObjectType.BUBBLE,
            color = GameObjectColor.RED,
            score = 1,
            speed = 1f,
            scale = 1f,
            velocity = floatArrayOf(1f, 0f, 0f),
            x = boundaries.left - 0.5f,
        )
        val engine = StaticMatchEngine(boundaries, MatchState(arrayListOf(enteringObject)))

        runSuspend { engine.advanceVisuals(0.1f) }

        assertEquals(listOf(enteringObject), engine.gameObjects)

        runSuspend { engine.advanceVisuals(20f) }

        assertEquals(emptyList(), engine.gameObjects)
    }

    private class StaticMatchEngine(boundaries: Boundaries, state: MatchState) :
        MatchEngine(boundaries, MatchEngineConfig("0".repeat(64)), state) {
        override suspend fun generateGameObjects(score: Int, elapsedMs: Long) = Unit
        override suspend fun updateCollectColor(score: Int, elapsedMs: Long) = Unit
    }

    private fun runSuspend(block: suspend () -> Unit) {
        var outcome: Result<Unit>? = null
        block.startCoroutine(object : Continuation<Unit> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) {
                outcome = result
            }
        })
        outcome!!.getOrThrow()
    }
}
