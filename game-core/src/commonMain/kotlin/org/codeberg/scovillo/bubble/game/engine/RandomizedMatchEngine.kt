package org.codeberg.scovillo.bubble.game.engine

import org.codeberg.scovillo.bubble.game.Boundaries
import org.codeberg.scovillo.bubble.game.GameObject
import org.codeberg.scovillo.bubble.game.GameObjectColor
import org.codeberg.scovillo.bubble.game.GameObjectType
import org.codeberg.scovillo.bubble.game.GameSpeed
import org.codeberg.scovillo.bubble.game.MatchState
import org.codeberg.scovillo.bubble.game.normalize
import kotlin.math.abs
import kotlin.random.Random
import kotlin.time.Clock

/** Non-replayable object generator used by the animated menu background. */
class RandomizedMatchEngine(
    boundaries: Boundaries,
    config: MatchEngineConfig,
    state: MatchState,
) : MatchEngine(boundaries, config, state) {
    private val randomSpawn = 0.05f
    private val delay = 6_000L
    private var timeFlag = currentTimeMs() + delay
    private val gameSpeed = GameSpeed()

    override suspend fun generateGameObjects(score: Int, elapsedMs: Long) {
        if (config.maxObjectsOnField <= state.activeGameObjects.size) return

        repeat(config.maxObjectsOnField - state.activeGameObjects.size) {
            val scale = config.minBubbleScale + Random.nextFloat() * config.bubbleScaleRange
            var spawnX = 0f
            var spawnZ = 0f
            val spawnOffset = scale * .5f
            val velocity = FloatArray(3)
            val sourceCode = (if (Random.nextBoolean()) 1 else 0) shl 1 or if (Random.nextBoolean()) 1 else 0
            val destinationCode = sourceCode xor 3

            if (Random.nextBoolean()) {
                spawnZ = if (sourceCode and 2 > 0) boundaries.bottom - spawnOffset else boundaries.top + spawnOffset
                spawnX = if (sourceCode and 1 > 0) boundaries.right * Random.nextFloat() else boundaries.left * Random.nextFloat()
            } else {
                spawnZ = if (sourceCode and 2 > 0) boundaries.bottom * Random.nextFloat() else boundaries.top * Random.nextFloat()
                spawnX = if (sourceCode and 1 > 0) boundaries.right + spawnOffset else boundaries.left - spawnOffset
            }
            if (Random.nextBoolean()) {
                velocity[2] = if (destinationCode and 2 > 0) boundaries.bottom - spawnOffset else boundaries.top + spawnOffset
                velocity[0] = if (destinationCode and 1 > 0) boundaries.right * Random.nextFloat() else boundaries.left * Random.nextFloat()
            } else {
                velocity[2] = if (destinationCode and 2 > 0) boundaries.bottom * Random.nextFloat() else boundaries.top * Random.nextFloat()
                velocity[0] = if (destinationCode and 1 > 0) boundaries.right + spawnOffset else boundaries.left - spawnOffset
            }
            velocity[0] -= spawnX
            velocity[2] -= spawnZ
            normalize(velocity)

            val positionOk = state.activeGameObjects.none { gameObject ->
                val minDistance = .5f * scale + .5f * gameObject.scale + config.minSpawnDistance
                abs(spawnX - gameObject.x) < minDistance && abs(spawnZ - gameObject.z) < minDistance
            }
            if (!positionOk) return@repeat

            val collectColorAvailable = state.activeGameObjects.any { gameObject ->
                gameObject.type == GameObjectType.BUBBLE && gameObject.color == currentCollectColor
            }
            val gameObject = if (Random.nextFloat() <= randomSpawn) {
                GameObject(GameObjectType.STAR, GameObjectColor.GOLD, 3, gameSpeed.getStarSpeedFor(score)).apply {
                    this.scale = scale * .85f
                }
            } else {
                val color = if (collectColorAvailable) generateColor() else currentCollectColor
                GameObject(GameObjectType.BUBBLE, color, 1, gameSpeed.getBubbleSpeedFor(score)).apply {
                    this.scale = scale
                }
            }
            gameObject.x = spawnX
            gameObject.y = 0f
            gameObject.z = spawnZ
            gameObject.velocity = velocity
            state.activeGameObjects += gameObject
        }
    }

    fun generateColor(): GameObjectColor =
        GameObjectColor.entries.filter { it != GameObjectColor.GOLD }.random()

    override suspend fun updateCollectColor(score: Int, elapsedMs: Long) {
        var collectColor = currentCollectColor
        if (currentTimeMs() >= timeFlag) {
            while (collectColor == currentCollectColor) collectColor = generateColor()
            val scoreScale = (1f - score / 400f).coerceAtLeast(.75f)
            timeFlag = currentTimeMs() + (delay * scoreScale).toLong()
        }
        currentCollectColor = collectColor
    }

    private fun currentTimeMs(): Long = Clock.System.now().toEpochMilliseconds()
}
