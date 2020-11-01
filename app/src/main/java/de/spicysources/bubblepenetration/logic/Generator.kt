package de.spicysources.bubblepenetration.logic

import de.spicysources.bubblepenetration.objects.Bubble
import de.spicysources.bubblepenetration.objects.GameObject
import de.spicysources.bubblepenetration.objects.Star
import de.spicysources.bubblepenetration.screen.Boundaries
import de.spicysources.bubblepenetration.util.BubbleColors
import de.spicysources.bubblepenetration.util.normalize
import java.util.*

class Generator(private val gameObjects: MutableList<GameObject>, private val boundaries: Boundaries) {

    private val colorCast = HashMap<BubbleColors, FloatArray>()

    private val maxObjectCountOnScreen = 20

    private var scale = 1.0f
    private val minScale = 0.75f
    private val maxScale = 0.9f

    // factor for randomizing spawns: Star < randomSpawn < Bubble
    private val randomSpawn = 0.05f
    var minSpawnDistanceBetweenObstacles = 1.5f

    // delay for changing collect color
    private val delay = 6000 //[ms]
    private var timeFlag = System.currentTimeMillis() + delay

    private val gameSpeed = GameSpeed()

    init {
        colorCast[BubbleColors.RED] = floatArrayOf(1.0f, 0.0f, 0.0f, 0.7f)
        colorCast[BubbleColors.ORANGE] = floatArrayOf(1.0f, 0.5f, 0.0f, 0.7f)
        colorCast[BubbleColors.GREEN] = floatArrayOf(0.0f, 1.0f, 0.0f, 0.7f)
        colorCast[BubbleColors.LIGHTBLUE] = floatArrayOf(0.0f, 1.0f, 1.0f, 0.7f)
        colorCast[BubbleColors.BLUE] = floatArrayOf(0.0f, 0.0f, 1.0f, 0.7f)
    }

    fun generateGameobject(collectColor: BubbleColors?, score: Int) {
        // Spawn new bubble to match the target obstacle count
        if (maxObjectCountOnScreen > gameObjects.size) {
            for (i in 0 until maxObjectCountOnScreen - gameObjects.size) {
                // determine what kind of obstacle is spawned next
                scale = Math.random().toFloat() * (maxScale - minScale) + minScale
                var spawnX = 0.0f
                var spawnZ = 0.0f
                val spawnOffset = scale * 0.5f
                val velocity = FloatArray(3)
                // determine source and destination quadrant
                val sourceCode =
                    (if (Math.random() < 0.5) 0 else 1) shl 1 or if (Math.random() < 0.5) 0 else 1 // source quadrant
                val destCode = sourceCode xor 3 // destination quadrant is opposite of source
                //Log.d("Code", sourceCode+" "+destCode);

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
                // calculate velocity
                velocity[0] -= spawnX
                velocity[2] -= spawnZ
                normalize(velocity)
                var positionOk = true
                // check distance to other gameobjects
                for (gameObject in gameObjects) {
                    val minDistance =
                        0.5f * scale + 0.5f * gameObject.scale + minSpawnDistanceBetweenObstacles
                    if (Math.abs(spawnX - gameObject.x) < minDistance
                        && Math.abs(spawnZ - gameObject.z) < minDistance
                    ) positionOk = false // Distance too small -> invalid position
                }
                if (!positionOk) continue  // Invalid spawn position -> try again next time
                //Is the needed color available?
                var collectColorAvailable = false
                for (gameObject in gameObjects) {
                    if (gameObject is Bubble && gameObject.color == collectColor) {
                        collectColorAvailable = true
                        break
                    }
                }
                //spawn new gameobject
                if (Math.random() <= randomSpawn) {
                    val newStar = Star(gameSpeed.getStarSpeedFor(score))
                    //stars a little bit smaller than Bubbles in average
                    newStar.scale = scale * 0.85f
                    newStar.setPosition(spawnX, 0f, spawnZ)
                    newStar.velocity = velocity
                    gameObjects.add(newStar)
                } else {
                    var newBubble: Bubble
                    //make sure there is a bubble with color to collect
                    newBubble = if (collectColorAvailable) {
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

    //Randomizes color value
    fun generateColor(): BubbleColors {
        return BubbleColors.values()[(Math.random() * BubbleColors.values().size).toInt()]
    }

    //Randomizes color to be collected
    fun generateCollectColor(currentColor: BubbleColors, score: Int): BubbleColors {
        var collectColor = currentColor
        // if time exceeds delay, change Color
        if (System.currentTimeMillis() >= timeFlag) {
            //make sure color is changing
            while (collectColor == currentColor) collectColor = generateColor()
            // more score means faster change, max at 300 score (4500[ms])
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