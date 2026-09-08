package org.codeberg.scovillo.bubble.game

import org.codeberg.scovillo.bubble.game.generator.DeterministicMatchEngine
import org.codeberg.scovillo.bubble.game.generator.MatchEngineConfig
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DeterministicMatchEngineTest {

    private val boundaries =
        Boundaries().apply { update(desiredHeight = 10f, aspectRatio = 1f) }
    private val state = MatchState(timerValueMs = 25f, score = 0)
    private val generator = DeterministicMatchEngine(
        config = MatchEngineConfig("00".repeat(32)),
        state = state,
        boundaries = boundaries,
    )

    @Test
    fun `object generation matches backend replay vector`() {
        val objectSpec = generator.objectAt(index = 17, scoreAtSpawn = 42)

        assertEquals("o_h_0179aa29", objectSpec.objectId)
        assertEquals(DeterministicMatchEngine.ObjectKind.BUBBLE, objectSpec.kind)
        assertEquals(BubbleColor.BLUE, objectSpec.color)
        assertEquals(5_100, objectSpec.spawnAtMs)
        assertEquals(12_015, objectSpec.expiresAtMs)
        assertEquals(0.89, objectSpec.scale, 1e-15)
        assertEquals(0.089, objectSpec.radiusX, 1e-15)
        assertEquals(0.089, objectSpec.radiusY, 1e-15)
        assertEquals(0.16227969787136645, objectSpec.startX, 1e-15)
        assertEquals(-0.0445, objectSpec.startY, 1e-15)
        assertEquals(0.7061722743572137, objectSpec.endX, 1e-15)
        assertEquals(1.0445, objectSpec.endY, 1e-15)
        assertEquals(1.3014805349908283, objectSpec.speed, 1e-15)
    }

    @Test
    fun `star scale matches the original game and backend replay vector`() {
        val star = generator.objectAt(index = 13, scoreAtSpawn = 0)

        assertEquals(DeterministicMatchEngine.ObjectKind.STAR, star.kind)
        assertEquals(0.756, star.scale, 1e-15)
        assertEquals(0.0756, star.radiusX, 1e-15)
        assertEquals(0.0756, star.radiusY, 1e-15)
        assertEquals(-0.0378, star.startX, 1e-15)
        assertEquals(1.0378, star.endX, 1e-15)
    }

    @Test
    fun `portrait viewport keeps render scale while adapting normalized hit radii`() {
        val portraitObject = generator.objectAt(
            index = 17,
            scoreAtSpawn = 42,
            viewportAspectRatio = 0.5625f,
        )

        assertEquals(0.89, portraitObject.scale, 1e-15)
        assertEquals(0.089, portraitObject.radiusX, 1e-15)
        assertEquals(0.0500625, portraitObject.radiusY, 1e-15)
        assertEquals(-0.02503125, portraitObject.startY, 1e-15)
        assertEquals(1.02503125, portraitObject.endY, 1e-15)
    }

    @Test
    fun `target color generation matches backend replay vector`() {
        assertEquals(BubbleColor.RED, generator.determineCollectColor(3, BubbleColor.BLUE))
    }

    @Test
    fun `a spawned bubble uses the target color when none is on the field`() {
        val spec = generator.objectForField(
            index = 17,
            scoreAtSpawn = 42,
            viewportAspectRatio = 1.0f,
            hasTargetBubble = false,
        )

        assertEquals(DeterministicMatchEngine.ObjectKind.BUBBLE, spec.kind)
        assertEquals(BubbleColor.RED, spec.color)
    }

    @Test
    fun `an available target bubble leaves the seeded color unchanged`() {
        val spec = generator.objectForField(
            index = 17,
            scoreAtSpawn = 42,
            viewportAspectRatio = 1.0f,
            hasTargetBubble = true,
        )

        assertEquals(BubbleColor.BLUE, spec.color)
    }

    @Test
    fun `a star becomes the current target when none is available`() {
        val spec = generator.objectForField(
            index = 13,
            scoreAtSpawn = 0,
            viewportAspectRatio = 1.0f,
            hasTargetBubble = false,
        )

        assertEquals(DeterministicMatchEngine.ObjectKind.BUBBLE, spec.kind)
        assertEquals(BubbleColor.RED, spec.color)
    }

}
