package de.spicysources.bubblepenetration.screen

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.opengl.GLSurfaceView
import android.opengl.GLU
import android.view.MotionEvent
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import de.spicysources.bubblepenetration.MainActivity
import de.spicysources.bubblepenetration.R
import de.spicysources.bubblepenetration.logic.Combo
import de.spicysources.bubblepenetration.objects.Bubble
import de.spicysources.bubblepenetration.objects.GameObject
import de.spicysources.bubblepenetration.util.BubbleColors
import de.spicysources.bubblepenetration.sound.SoundEffects
import de.spicysources.bubblepenetration.logic.Generator
import de.spicysources.bubblepenetration.logic.Time
import de.spicysources.bubblepenetration.objects.Star
import de.spicysources.bubblepenetration.screen.hud.ScorePostfix
import de.spicysources.bubblepenetration.screen.hud.TimerPostfix
import de.spicysources.bubblepenetration.util.roundToFirstDigit
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.opengles.GL11

class BubbleGLSurfaceView(context: Context) : GLSurfaceView(context) {

    private val mainActivity = context as MainActivity
    private val assets = context.assets

    private val effectPlayer = SoundEffects(context)

    private var alarmed = false
    private val boundaries = Boundaries()
    private val gameObjects = ArrayList<GameObject>()
    private val generator = Generator(gameObjects, boundaries)
    private var collectColor = BubbleColors.RED
    private var timer = 20.0f
    private var score = 0
    private var isTouch = false
    private val objectsToBeRemoved = ArrayList<GameObject>()
    private val targetsToBeRemoved = ArrayList<GameObject>()
    private val timerText: TextView
    private var scoreText: TextView
    private val renderer: BubbleRenderer
    private val timeLogic = Time()
    private val combo = Combo(mainActivity)

    init {
        timerText = mainActivity.findViewById<View>(R.id.Timer) as TextView
        timerText.typeface = Typeface.createFromAsset(this.assets, "fonts/PLUMP.ttf")

        scoreText = mainActivity.findViewById<View>(R.id.Score) as TextView
        scoreText.typeface = Typeface.createFromAsset(this.assets, "fonts/PLUMP.ttf")

        renderer = BubbleRenderer()
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
                    val x = event.x * renderer.unitsPerPixelX - boundaries.right - bubble.x
                    val y = (event.y * renderer.unitsPerPixelZ - boundaries.top) * -1 - bubble.z
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

    private inner class BubbleRenderer : Renderer {

        private val modelViewScene = FloatArray(16)
        var lastFrameTime = System.currentTimeMillis()

        var unitsPerPixelX = 0f
            private set
        var unitsPerPixelZ = 0f
            private set

        private val timerTextAnimation: Animation = AlphaAnimation(0.35f, 1.0f)
        private val timerPostfix = TimerPostfix(mainActivity)
        private val scorePostfix = ScorePostfix(mainActivity)

        init {
            timerTextAnimation.duration = 300
            timerTextAnimation.startOffset = 20
            timerTextAnimation.repeatMode = Animation.REVERSE
            timerTextAnimation.repeatCount = Animation.INFINITE
        }

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
            mainActivity.runOnUiThread {
                if (alarmed) {
                    if (timerText.animation == null) {
                        timerText.startAnimation(timerTextAnimation)
                    }
                }
                if (timer > 10) {
                    timerText.animation?.cancel()
                    alarmed = false
                }
                if (timer <= 0.0) {
                    effectPlayer.ingame = false
                    mainActivity.showGameOverScreenWith(score.toString())
                } else {
                    timerText.text = "time: " + roundToFirstDigit(timer)
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
            gameObjects.forEach { it.draw(gl) }
        }

        private fun updateGameobjects(fracSec: Float) {

            gameObjects.forEach {
                it.update(fracSec)
                // offset makes sure that the gameobjects don't get deleted or set
                // inactive while visible to the player.
                val offset = it.scale
                if (it.x > boundaries.right + offset
                    || it.x < boundaries.left - offset
                    || it.z > boundaries.top + offset
                    || it.z < boundaries.bottom - offset
                ) {
                    objectsToBeRemoved.add(it)
                }
            }
            for (gameObject in targetsToBeRemoved) {
                if (isTouch) {
                    break
                }
                // collected bubble or star
                when (gameObject) {
                    is Bubble -> {
                        //Check if hit bubble have the right color
                        if (gameObject.color == collectColor) {
                            val time = timeLogic.getBubbleTimeFor(gameObject.speed)
                            timer += time
                            timerPostfix.animateWith(time)

                            val collectScore = gameObject.score * combo.multiplikator
                            score += collectScore
                            scorePostfix.animateWith(collectScore)

                            combo.increment()
                            effectPlayer.playSound(R.raw.blubb)
                        } else {
                            val time = -timeLogic.getBubblePunishmentTimeFor(gameObject.speed)
                            timer += time
                            timerPostfix.animateWith(time)
                            combo.reset()
                            effectPlayer.playSound(R.raw.fart)
                        }
                    }
                    is Star -> {
                        val time = timeLogic.getStarTimeFor(gameObject.speed)
                        timer += time
                        timerPostfix.animateWith(time)

                        val collectScore = gameObject.score * combo.multiplikator
                        score += collectScore
                        scorePostfix.animateWith(collectScore)

                        effectPlayer.playSound(R.raw.star)
                    }
                }
                combo.update()
                gameObjects.remove(gameObject)
            }
            targetsToBeRemoved.clear()
            for (gameObject in objectsToBeRemoved) {
                if (isTouch) {
                    break
                }
                gameObjects.remove(gameObject)
            }
            objectsToBeRemoved.clear()
            //spawn new gameobjects
            generator.generateGameobject(collectColor, score)
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
            val desiredHeight = 10.0f
            // We want to be able to see the range of 5 to -5 units at the y
            // axis (height=10).
            // To achieve this we have to pull the camera towards the positive z axis
            // based on the following formula:
            // z = (desired_height / 2) / tan(fovy/2)
            val z =
                (desiredHeight / 2 / Math.tan(fovy / 2 * (Math.PI / 180.0f))).toFloat()
            // forward for the camera is backward for the scene
            gl.glTranslatef(0.0f, 0.0f, -z)
            // rotate local to achive top down view from negative y down to xz-plane
            // z range is the desired height
            gl.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f)
            // save local system as a basis to draw scene items
            gl11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, modelViewScene, 0)
            // window boundaries
            boundaries.updateWith(desiredHeight, aspectRatio)
            // tochevent pixel coordinates to openGL coordinates
            unitsPerPixelZ = desiredHeight / height
            unitsPerPixelX = desiredHeight * aspectRatio / width
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