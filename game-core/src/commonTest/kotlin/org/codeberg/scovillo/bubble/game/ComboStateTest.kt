package org.codeberg.scovillo.bubble.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ComboStateTest {
    @Test
    fun activatesAfterSixCorrectCollections() {
        val state = ComboState()

        repeat(6) { state.increment(it * 100L) }

        assertEquals(2, state.multiplier)
        assertTrue(state.isActive)
    }

    @Test
    fun expiresOnlyAfterTheConfiguredTimeout() {
        val state = ComboState(durationMs = 100L)
        state.increment(10L)

        assertFalse(state.expireAt(110L))
        assertTrue(state.expireAt(111L))
        assertEquals(1, state.multiplier)
        assertEquals(0, state.progress)
    }
}
