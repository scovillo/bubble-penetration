package de.spicysources.bubblepenetration.screen

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.opengl.GLSurfaceView
import android.opengl.GLU
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import de.spicysources.bubblepenetration.MainActivity
import de.spicysources.bubblepenetration.R
import de.spicysources.bubblepenetration.animation.BlinkAnimation
import de.spicysources.bubblepenetration.objects.Bubble
import de.spicysources.bubblepenetration.objects.GameObject
import de.spicysources.bubblepenetration.util.BubbleColors
import de.spicysources.bubblepenetration.sound.SoundEffects
import de.spicysources.bubblepenetration.util.Generator
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.opengles.GL11

class BubbleGLSurfaceView(context: Context) : GLSurfaceView(context) {

    private val mainActivity = context as MainActivity
    private val assets = context.assets
    private val generator = Generator()
    private val effectPlayer = SoundEffects(context)

    private var alarmed = false
    var boundaryTop = 0f
    var boundaryBottom = 0f
    var boundaryLeft = 0f
    var boundaryRight = 0f
    private var collectColor = BubbleColors.RED
    private var timer = 40.0f
    private var score = 0
    private var isTouch = false
    private val gameObjects = ArrayList<GameObject>()
    private val objectsToBeRemoved = ArrayList<GameObject>()
    private val targetsToBeRemoved = ArrayList<GameObject>()
    private val timerText: TextView
    private var scoreText: TextView
    private val renderer: BubbleRenderer

    // game balance factors
    private val starScore = 5
    private val starTime = 5
    private val bubbleScore = 2
    private val bubbleTime = 2
    private val increaseSpeed = 0.02f

    init {
        timerText = mainActivity.findViewById<View>(R.id.Timer) as TextView
        timerText.typeface = Typeface.createFromAsset(this.assets, "fonts/PLUMP.ttf")
        scoreText = mainActivity.findViewById<View>(R.id.Score) as TextView
        scoreText.typeface = Typeface.createFromAsset(this.assets, "fonts/PLUMP.ttf")
        renderer = BubbleRenderer(timerText)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        Thread(effectPlayer).start()
    }

    fun isMuted(value: Boolean) {
        effectPlayer.isMuted = value
    }

    //Collect Bubbles
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                //Checks if Object is hit by touch
                //isTouch locks remove loops in update method as long as the touch event is executet
                isTouch = true
                var targetIndex = -1
                var targetCounter = 0
                var distance = 0.0
                var i = 0
                while (i < gameObjects.size) {
                    val bubble = gameObjects[i]
                    val x = event.x * renderer.unitsPerPixelX - boundaryRight - bubble.x
                    val y =
                        (event.y * renderer.unitsPerPixelZ - boundaryTop) * -1 - bubble.z
                    if (Math.sqrt(
                            Math.pow(x.toDouble(), 2.0) + Math.pow(
                                y.toDouble(),
                                2.0
                            )
                        ) <= bubble.scale
                    ) {
                        if (distance <= 1E-20) {
                            distance = Math.sqrt(
                                Math.pow(
                                    x.toDouble(),
                                    2.0
                                ) + Math.pow(y.toDouble(), 2.0)
                            )
                            targetIndex = targetCounter
                        } else {
                            if (Math.sqrt(
                                    Math.pow(
                                        x.toDouble(),
                                        2.0
                                    ) + Math.pow(y.toDouble(), 2.0)
                                ) < distance
                            ) {
                                distance = Math.sqrt(
                                    Math.pow(
                                        x.toDouble(),
                                        2.0
                                    ) + Math.pow(y.toDouble(), 2.0)
                                )
                                targetIndex = targetCounter
                            }
                        }
                    }
                    targetCounter++
                    i++
                }
                if (targetIndex >= 0) {
                    targetsToBeRemoved.add(gameObjects[targetIndex])
                }
                isTouch = false
            }
        }
        return true
    }

    private inner class BubbleRenderer(timerText: TextView) : Renderer {

        private val modelViewScene = FloatArray(16)
        var lastFrameTime = System.currentTimeMillis()

        var unitsPerPixelX = 0f
            private set
        var unitsPerPixelZ = 0f
            private set

        private val timerBlinkAnimation = BlinkAnimation(timerText)

        override fun onDrawFrame(gl: GL10) {
            // update time calculation
            val delta = System.currentTimeMillis() - lastFrameTime
            val fracSec = delta.toFloat() / 1000
            lastFrameTime = System.currentTimeMillis()

            // scene updates
            if (timer < 10 && !alarmed) {
                effectPlayer.playSound(R.raw.alarm)
                alarmed = true
            }
            if (timer - fracSec <= 0.0) {
                timer = 0.0f
            } else {
                timer -= fracSec
            }
            // update color to collect
            collectColor = generator.generateCollectColor(collectColor, score)
            // refresh HUD
            (context as MainActivity?)!!.runOnUiThread {
                if (alarmed) {
                    timerBlinkAnimation.update()
                }
                if (timer > 10) {
                    timerBlinkAnimation.stop()
                    alarmed = false
                }
                if (timer <= 0.0) {
                    effectPlayer.ingame = false
                    mainActivity.showGameOverScreenWith(score.toString())
                } else {
                    timerText.text = "time: " + timeToTimeFormat(timer.toDouble(), 1)
                    scoreText.text = "score: $score"
                    setHUDColor(generator.getGLColor(collectColor))
                }
            }
            //update gameobjects
            updateGameobjects(fracSec)
            // clear screen and depth buffer
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)
            val gl11 = gl as GL11

            // load local system to draw scene items
            gl.glMatrixMode(GL10.GL_MODELVIEW)
            gl11.glLoadMatrixf(modelViewScene, 0)
            //draw gameobjects
            for (`object` in gameObjects) {
                `object`.draw(gl)
            }
        }

        private fun updateGameobjects(fracSec: Float) {
            // position update on all gameobjects
            for (`object` in gameObjects) {
                `object`.update(fracSec)
            }
            // check for gameobjects that flew out of the viewing area and remove
            // or deactivate them
            for (`object` in gameObjects) {
                // offset makes sure that the gameobjects don't get deleted or set
                // inactive while visible to the player.
                val offset = `object`.scale
                if (`object`.x > boundaryRight + offset
                    || `object`.x < boundaryLeft - offset
                    || `object`.z > boundaryTop + offset
                    || `object`.z < boundaryBottom - offset
                ) {
                    objectsToBeRemoved.add(`object`)
                }
            }
            for (`object` in targetsToBeRemoved) {
                if (isTouch) {
                    break
                }
                // collected bubble or star
                if (`object` is Bubble) {
                    //Check if hit bubble have the right color
                    if (`object`.color == collectColor) {
                        timer += bubbleTime.toFloat()
                        score += bubbleScore
                        GameObject.speed += increaseSpeed
                        effectPlayer.playSound(R.raw.blubb)
                    } else {
                        timer -= bubbleScore * 2.toFloat()
                        effectPlayer.playSound(R.raw.fart)
                    }
                } else {
                    timer += starTime.toFloat()
                    score += starScore
                    GameObject.speed += increaseSpeed * 2
                    effectPlayer.playSound(R.raw.star)
                }
                gameObjects.remove(`object`)
            }
            targetsToBeRemoved.clear()
            // remove obsolete gameobjects
            for (`object` in objectsToBeRemoved) {
                if (isTouch) {
                    break
                }
                gameObjects.remove(`object`)
            }
            objectsToBeRemoved.clear()
            //spawn new gameobjects
            generator.generateGameobject(
                gameObjects,
                collectColor,
                boundaryBottom,
                boundaryTop,
                boundaryRight,
                boundaryLeft
            )
        }

        // Called when surface is created or the viewport gets resized
        // set projection matrix
        // precalculate modelview matrix
        override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
            val gl11 = gl as GL11
            gl.glViewport(0, 0, width, height)
            val aspectRatio = width.toFloat() / height
            val fovy = 45.0f
            // set up projection matrix for scene
            gl.glMatrixMode(GL10.GL_PROJECTION)
            gl.glLoadIdentity()
            GLU.gluPerspective(gl, fovy, aspectRatio, 0.001f, 100.0f)
            // set up modelview matrix for scene
            gl.glMatrixMode(GL10.GL_MODELVIEW)
            gl.glLoadIdentity()
            val desired_height = 10.0f
            // We want to be able to see the range of 5 to -5 units at the y
            // axis (height=10).
            // To achieve this we have to pull the camera towards the positive z axis
            // based on the following formula:
            // z = (desired_height / 2) / tan(fovy/2)
            val z =
                (desired_height / 2 / Math.tan(fovy / 2 * (Math.PI / 180.0f))).toFloat()
            // forward for the camera is backward for the scene
            gl.glTranslatef(0.0f, 0.0f, -z)
            // rotate local to achive top down view from negative y down to xz-plane
            // z range is the desired height
            gl.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f)
            // save local system as a basis to draw scene items
            gl11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, modelViewScene, 0)
            // window boundaries
            // z range is the desired height
            boundaryTop = desired_height / 2
            boundaryBottom = -desired_height / 2
            // x range is the desired width
            boundaryLeft = -(desired_height / 2 * aspectRatio)
            boundaryRight = desired_height / 2 * aspectRatio
            // tochevent pixel coordinates to openGL coordinates
            unitsPerPixelZ = desired_height / height
            unitsPerPixelX = desired_height * aspectRatio / width
        }

        override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
            gl.glDisable(GL10.GL_DITHER)
            gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST)
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
            gl.glEnable(GL10.GL_BLEND)
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
            gl.glEnable(GL10.GL_CULL_FACE)
            gl.glShadeModel(GL10.GL_FLAT)
            gl.glEnable(GL10.GL_DEPTH_TEST)
            gl.glDepthFunc(GL10.GL_LEQUAL)
            gl.glShadeModel(GL10.GL_SMOOTH)
            gl.glEnable(GL10.GL_DEPTH_TEST)
        }

        private fun timeToTimeFormat(time: Double, nachkommastellen: Int): Double {
            val factor = Math.pow(10.0, nachkommastellen.toDouble())
            return (time * factor).toInt().toDouble() / factor
        }


        private fun setHUDColor(glCollectColor: FloatArray) {
            val max = 255
            mainActivity.findViewById<View>(R.id.hud).setBackgroundColor(
                Color.argb(
                    (glCollectColor[3] * max).toInt(), (glCollectColor[0] * max).toInt(),
                    (glCollectColor[1] * max).toInt(), (glCollectColor[2] * max).toInt()
                )
            )
        }

    }

}