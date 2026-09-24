package org.codeberg.scovillo.bubble.android.ui.render

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Transfers touch input from Android's UI thread to the OpenGL simulation thread.
 *
 * A match must only mutate its engine on that simulation thread; otherwise the
 * recorded replay can describe a different state than the one that accepted a tap.
 */
class GameInputQueue {
    private val pendingInputs = ConcurrentLinkedQueue<GameInput>()

    fun enqueue(normalizedX: Double, normalizedY: Double) {
        pendingInputs.add(GameInput(normalizedX, normalizedY))
    }

    fun drain(handleInput: (GameInput) -> Unit) {
        while (true) {
            val input = pendingInputs.poll() ?: return
            handleInput(input)
        }
    }

    fun clear() {
        pendingInputs.clear()
    }
}

data class GameInput(
    val normalizedX: Double,
    val normalizedY: Double,
)
