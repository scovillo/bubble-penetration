package org.codeberg.scovillo.bubble.android.ui.render

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GameInputQueueTest {
    @Test
    fun drainsInputsInTheirSubmissionOrder() {
        val queue = GameInputQueue()
        queue.enqueue(0.2, 0.3)
        queue.enqueue(0.7, 0.8)

        val handled = mutableListOf<GameInput>()
        queue.drain(handled::add)

        assertEquals(
            listOf(GameInput(0.2, 0.3), GameInput(0.7, 0.8)),
            handled,
        )
    }

    @Test
    fun clearsInputsThatArrivedWhileTheMatchWasPaused() {
        val queue = GameInputQueue()
        queue.enqueue(0.2, 0.3)
        queue.clear()

        val handled = mutableListOf<GameInput>()
        queue.drain(handled::add)

        assertEquals(emptyList(), handled)
    }
}
