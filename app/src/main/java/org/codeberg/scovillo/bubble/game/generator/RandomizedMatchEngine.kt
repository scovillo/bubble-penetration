package org.codeberg.scovillo.bubble.game.generator

import org.codeberg.scovillo.bubble.game.Bubble
import org.codeberg.scovillo.bubble.game.BubbleColor
import org.codeberg.scovillo.bubble.game.Boundaries
import org.codeberg.scovillo.bubble.game.GameSpeed
import org.codeberg.scovillo.bubble.game.MatchState
import org.codeberg.scovillo.bubble.game.Star
import org.codeberg.scovillo.bubble.game.normalize
import kotlin.math.abs

class RandomizedMatchEngine(
    boundaries: Boundaries,
    config: MatchEngineConfig,
    state: MatchState,
) : MatchEngine(boundaries, config, state) {
    private val randomSpawn = 0.05f
    private val delay = 6000
    private var timeFlag = System.currentTimeMillis() + delay
    private val gameSpeed = GameSpeed()

    override fun generateGameObjects(score: Int, elapsedMs: Long) {
        if (config.maxObjectsOnField > state.activeGameObjects.size) {
            for (i in 0 until config.maxObjectsOnField - state.activeGameObjects.size) {
                val scale =
                    config.minBubbleScale + Math.random().toFloat() * config.bubbleScaleRange
                var spawnX = 0.0f
                var spawnZ = 0.0f
                val spawnOffset = scale * 0.5f
                val velocity = FloatArray(3)
                val sourceCode =
                    (if (Math.random() < 0.5) 0 else 1) shl 1 or if (Math.random() < 0.5) 0 else 1
                val destCode = sourceCode xor 3 // destination quadrant is opposite of source
                /* sourceCode, destCode
					 * +----+----+
					 * | 00 | 01 |
					 * +----+----+
					 * | 10 | 11 |
					 * +----+----+
					 */
                // calculate source vertex position, <0.5 horizontal, else vertical
                if (Math.random() < 0.5) {  // horizontal placing, top or bottom
                    spawnZ =
                        (if (sourceCode and 2 > 0) boundaries.bottom - spawnOffset else boundaries.top + spawnOffset).toFloat()
                    spawnX =
                        if (sourceCode and 1 > 0) boundaries.right * Math.random()
                            .toFloat() else boundaries.left * Math.random()
                            .toFloat()
                } else {  // vertical placing, left or right
                    spawnZ =
                        if (sourceCode and 2 > 0) boundaries.bottom * Math.random()
                            .toFloat() else boundaries.top * Math.random()
                            .toFloat()
                    spawnX =
                        (if (sourceCode and 1 > 0) boundaries.right + spawnOffset else boundaries.left - spawnOffset).toFloat()
                }
                // calculate destination vertex position, <0.5 horizontal, else vertical
                if (Math.random() < 0.5) {  // horizontal placing, top or bottom
                    velocity[2] =
                        (if (destCode and 2 > 0) boundaries.bottom - spawnOffset else boundaries.top + spawnOffset).toFloat()
                    velocity[0] =
                        (if (destCode and 1 > 0) boundaries.right * Math.random() else boundaries.left * Math.random()).toFloat()
                } else {  // vertical placing, left or right
                    velocity[2] =
                        (if (destCode and 2 > 0) boundaries.bottom * Math.random() else boundaries.top * Math.random()).toFloat()
                    velocity[0] =
                        (if (destCode and 1 > 0) boundaries.right + spawnOffset else boundaries.left - spawnOffset).toFloat()
                }
                velocity[0] -= spawnX
                velocity[2] -= spawnZ
                normalize(velocity)
                var positionOk = true
                for (gameObject in state.activeGameObjects) {
                    val minDistance =
                        0.5f * scale + 0.5f * gameObject.scale + config.minSpawnDistance
                    if (abs(spawnX - gameObject.x) < minDistance
                        && abs(spawnZ - gameObject.z) < minDistance
                    ) positionOk = false
                }
                if (!positionOk) continue
                var collectColorAvailable = false
                for (gameObject in state.activeGameObjects) {
                    if (gameObject is Bubble && gameObject.color == currentCollectColor) {
                        collectColorAvailable = true
                        break
                    }
                }
                if (Math.random() <= randomSpawn) {
                    val newStar = Star(gameSpeed.getStarSpeedFor(score))
                    newStar.scale = scale * 0.85f
                    newStar.setPosition(spawnX, 0f, spawnZ)
                    newStar.velocity = velocity
                    state.activeGameObjects.add(newStar)
                } else {
                    val newBubble: Bubble = if (collectColorAvailable) {
                        val random = generateColor()
                        Bubble(random, random.rgb(), gameSpeed.getBubbleSpeedFor(score))
                    } else Bubble(
                        currentCollectColor,
                        currentCollectColor.rgb(),
                        gameSpeed.getBubbleSpeedFor(score)
                    )
                    newBubble.scale = scale
                    newBubble.setPosition(spawnX, 0f, spawnZ)
                    newBubble.velocity = velocity
                    state.activeGameObjects.add(newBubble)
                }
            }
        }
    }

    fun generateColor(): BubbleColor {
        return BubbleColor.entries[(Math.random() * BubbleColor.entries.size).toInt()]
    }

    override fun updateCollectColor(score: Int, elapsedMs: Long) {
        var collectColor = currentCollectColor
        if (System.currentTimeMillis() >= timeFlag) {
            while (collectColor == currentCollectColor) collectColor = generateColor()
            var scoreScale = 1 - score / 400.toFloat()
            if (scoreScale < 0.75f) scoreScale = 0.75f
            timeFlag = System.currentTimeMillis() + (delay * scoreScale).toInt()
        }
        currentCollectColor = collectColor
    }
}
