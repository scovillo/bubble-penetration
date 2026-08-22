package org.codeberg.scovillo.bubble.game

import org.codeberg.scovillo.bubble.ui.Boundaries

class Generator(
    private val gameObjects: MutableList<GameObject>,
    private val boundaries: Boundaries
) {

    private val colorCast = HashMap<BubbleColors, FloatArray>()

    private val maxObjectCountOnScreen = 20

    private var scale = 1.0f
    private val minScale = 0.75f
    private val maxScale = 0.9f
    private val randomSpawn = 0.05f
    var minSpawnDistanceBetweenObstacles = 1.5f
    private val delay = 6000
    private var timeFlag = System.currentTimeMillis() + delay

    private val gameSpeed = GameSpeed()

    init {
        colorCast[BubbleColors.RED] = floatArrayOf(1.0f, 0.0f, 0.0f, 0.7f)
        colorCast[BubbleColors.PURPLE] = floatArrayOf(0.75f, 0.0f, 1.0f, 0.7f)
        colorCast[BubbleColors.GREEN] = floatArrayOf(0.0f, 0.75f, 0.0f, 0.7f)
        colorCast[BubbleColors.LIGHTBLUE] = floatArrayOf(0.0f, 1.0f, 1.0f, 0.7f)
        colorCast[BubbleColors.BLUE] = floatArrayOf(0.0f, 0.0f, 1.0f, 0.7f)
    }

    fun generateGameobject(collectColor: BubbleColors?, score: Int) {
        if (maxObjectCountOnScreen > gameObjects.size) {
            for (i in 0 until maxObjectCountOnScreen - gameObjects.size) {
                scale = Math.random().toFloat() * (maxScale - minScale) + minScale
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
                    spawnZ = if (sourceCode and 2 > 0) boundaries.bottom - spawnOffset else boundaries.top + spawnOffset
                    spawnX =
                        if (sourceCode and 1 > 0) boundaries.right * Math.random()
                            .toFloat() else boundaries.left * Math.random()
                            .toFloat()
                } else {  // vertical placing, left or right
                    spawnZ =
                        if (sourceCode and 2 > 0) boundaries.bottom * Math.random()
                            .toFloat() else boundaries.top * Math.random()
                            .toFloat()
                    spawnX = if (sourceCode and 1 > 0) boundaries.right + spawnOffset else boundaries.left - spawnOffset
                }
                // calculate destination vertex position, <0.5 horizontal, else vertical
                if (Math.random() < 0.5) {  // horizontal placing, top or bottom
                    velocity[2] =
                        if (destCode and 2 > 0) boundaries.bottom - spawnOffset else boundaries.top + spawnOffset
                    velocity[0] =
                        if (destCode and 1 > 0) boundaries.right * Math.random()
                            .toFloat() else boundaries.left * Math.random()
                            .toFloat()
                } else {  // vertical placing, left or right
                    velocity[2] =
                        if (destCode and 2 > 0) boundaries.bottom * Math.random()
                            .toFloat() else boundaries.top * Math.random()
                            .toFloat()
                    velocity[0] =
                        if (destCode and 1 > 0) boundaries.right + spawnOffset else boundaries.left - spawnOffset
                }
                velocity[0] -= spawnX
                velocity[2] -= spawnZ
                normalize(velocity)
                var positionOk = true
                for (gameObject in gameObjects) {
                    val minDistance =
                        0.5f * scale + 0.5f * gameObject.scale + minSpawnDistanceBetweenObstacles
                    if (Math.abs(spawnX - gameObject.x) < minDistance
                        && Math.abs(spawnZ - gameObject.z) < minDistance
                    ) positionOk = false
                }
                if (!positionOk) continue
                var collectColorAvailable = false
                for (gameObject in gameObjects) {
                    if (gameObject is Bubble && gameObject.color == collectColor) {
                        collectColorAvailable = true
                        break
                    }
                }
                if (Math.random() <= randomSpawn) {
                    val newStar = Star(gameSpeed.getStarSpeedFor(score))
                    newStar.scale = scale * 0.85f
                    newStar.setPosition(spawnX, 0f, spawnZ)
                    newStar.velocity = velocity
                    gameObjects.add(newStar)
                } else {
                    val newBubble: Bubble = if (collectColorAvailable) {
                        val random = generateColor()
                        Bubble(random, colorCast[random]!!, gameSpeed.getBubbleSpeedFor(score))
                    } else Bubble(collectColor, colorCast[collectColor]!!, gameSpeed.getBubbleSpeedFor(score))
                    newBubble.scale = scale
                    newBubble.setPosition(spawnX, 0f, spawnZ)
                    newBubble.velocity = velocity
                    gameObjects.add(newBubble)
                }
            }
        }
    }

    fun generateColor(): BubbleColors {
        return BubbleColors.entries[(Math.random() * BubbleColors.entries.size).toInt()]
    }

    fun generateCollectColor(currentColor: BubbleColors, score: Int): BubbleColors {
        var collectColor = currentColor
        if (System.currentTimeMillis() >= timeFlag) {
            while (collectColor == currentColor) collectColor = generateColor()
            var scoreScale = 1 - score / 400.toFloat()
            if (scoreScale < 0.75f) scoreScale = 0.75f
            timeFlag = System.currentTimeMillis() + (delay * scoreScale).toInt()
        }
        return collectColor
    }

    fun getGLColor(color: BubbleColors): FloatArray {
        return colorCast[color]!!
    }

}
