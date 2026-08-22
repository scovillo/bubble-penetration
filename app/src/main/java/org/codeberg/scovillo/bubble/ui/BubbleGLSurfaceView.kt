package org.codeberg.scovillo.bubble.ui

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
import org.codeberg.scovillo.bubble.MainActivity
import org.codeberg.scovillo.bubble.R
import org.codeberg.scovillo.bubble.game.Bubble
import org.codeberg.scovillo.bubble.game.BubbleColors
import org.codeberg.scovillo.bubble.game.Combo
import org.codeberg.scovillo.bubble.game.GameObject
import org.codeberg.scovillo.bubble.game.Generator
import org.codeberg.scovillo.bubble.game.Star
import org.codeberg.scovillo.bubble.game.Time
import org.codeberg.scovillo.bubble.sound.SoundEffects
import org.codeberg.scovillo.bubble.ui.hud.ScorePostfix
import org.codeberg.scovillo.bubble.ui.hud.TimerPostfix
import java.math.RoundingMode
import java.text.DecimalFormat
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.opengles.GL11
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.tan

class BubbleGLSurfaceView(context: Context) : GLSurfaceView(context) {

    private val mainActivity = context as MainActivity
    private val assets = context.assets

    private val effectPlayer = SoundEffects(context)

    private var alarmed = false
    private val boundaries = Boundaries()
    private val gameObjects = ArrayList<GameObject>()
    private val generator = Generator(gameObjects, boundaries)
    private var collectColor = BubbleColors.RED
    private var timer = 25.0f
    private var score = 0
    private var isTouch = false
    private val objectsToBeRemoved = ArrayList<GameObject>()
    private val targetsToBeRemoved = ArrayList<GameObject>()
    private val timerText: TextView = mainActivity.findViewById<View>(R.id.Timer) as TextView
    private var scoreText: TextView
    private val renderer: BubbleRenderer
    private val timeLogic = Time()
    private val combo = Combo(mainActivity, effectPlayer)
    private val firstDigitFormat = DecimalFormat("0.0")

    init {
        timerText.typeface = Typeface.createFromAsset(this.assets, "fonts/PLUMP.ttf")

        scoreText = mainActivity.findViewById<View>(R.id.Score) as TextView
        scoreText.typeface = Typeface.createFromAsset(this.assets, "fonts/PLUMP.ttf")

        firstDigitFormat.roundingMode = RoundingMode.CEILING

        renderer = BubbleRenderer()
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        Thread(effectPlayer).start()
    }

    fun isMuted(value: Boolean) {
        effectPlayer.isMuted = value
    }

    fun pauseGame() {
        onPause()
    }

    fun resumeGame() {
        renderer.lastFrameTime = System.currentTimeMillis()
        onResume()
    }

    fun getScore(): Int = score
    fun getTimer(): Float = timer

    fun setGameState(score: Int, timer: Float) {
        this.score = score
        this.timer = timer
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                isTouch = true
                var targetIndex = -1
                var targetCounter = 0
                var distance = 0.0
                var i = 0
                while (i < gameObjects.size) {
                    val bubble = gameObjects[i]
                    val x = event.x * renderer.unitsPerPixelX - boundaries.right - bubble.x
                    val y = (event.y * renderer.unitsPerPixelZ - boundaries.top) * -1 - bubble.z
                    if (sqrt(
                            x.toDouble().pow(2.0) + y.toDouble().pow(2.0)
                        ) <= bubble.scale
                    ) {
                        if (distance <= 1E-20) {
                            distance = sqrt(
                                x.toDouble().pow(2.0) + y.toDouble().pow(2.0)
                            )
                            targetIndex = targetCounter
                        } else {
                            if (sqrt(
                                    x.toDouble().pow(2.0) + y.toDouble().pow(2.0)
                                ) < distance
                            ) {
                                distance = sqrt(
                                    x.toDouble().pow(2.0) + y.toDouble().pow(2.0)
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
                    performClick()
                }
                isTouch = false
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
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
            val delta = System.currentTimeMillis() - lastFrameTime
            val fracSec = delta.toFloat() / 1000
            lastFrameTime = System.currentTimeMillis()
            if (timer < 10 && !alarmed) {
                effectPlayer.playSound(R.raw.alarm)
                alarmed = true
            }
            if (timer - fracSec <= 0.0) {
                timer = 0.0f
            } else {
                timer -= fracSec
            }
            collectColor = generator.generateCollectColor(collectColor, score)
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
                    timerText.text = context.getString(R.string.timer_value, firstDigitFormat.format(timer))
                    scoreText.text = context.getString(R.string.score_value, score)
                    setHUDColor(generator.getGLColor(collectColor))
                }
            }
            updateGameObjects(fracSec)
            combo.update()
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)
            val gl11 = gl as GL11
            gl.glMatrixMode(GL10.GL_MODELVIEW)
            gl11.glLoadMatrixf(modelViewScene, 0)
            gameObjects.forEach { it.draw(gl) }
        }

        private fun updateGameObjects(fracSec: Float) {

            gameObjects.forEach {
                it.update(fracSec)
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
                when (gameObject) {
                    is Bubble -> {
                        if (gameObject.color == collectColor) {
                            val time = timeLogic.getBubbleTimeFor(gameObject.speed)
                            timer += time
                            timerPostfix.animateWith(time)

                            val collectScore = gameObject.score * combo.multiplier
                            score += collectScore
                            scorePostfix.animateWith(collectScore)

                            combo.increment()
                            if (combo.isActive) {
                                combo.giveHapticFeedBack()
                            }
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

                        val collectScore = gameObject.score * combo.multiplier
                        score += collectScore
                        scorePostfix.animateWith(collectScore)

                        if(combo.isActive) {
                            combo.giveHapticFeedBack()
                        }
                        effectPlayer.playSound(R.raw.star)
                    }
                }
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
            generator.generateGameobject(collectColor, score)
        }

        override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
            val gl11 = gl as GL11
            gl.glViewport(0, 0, width, height)
            val aspectRatio = width.toFloat() / height
            val fovy = 45.0f
            gl.glMatrixMode(GL10.GL_PROJECTION)
            gl.glLoadIdentity()
            GLU.gluPerspective(gl, fovy, aspectRatio, 0.001f, 100.0f)
            gl.glMatrixMode(GL10.GL_MODELVIEW)
            gl.glLoadIdentity()
            val desiredHeight = if (aspectRatio > 1.0f) 10.0f else 10.0f / aspectRatio
            // We want to be able to see the range of 5 to -5 units at the y
            // axis (height=10).
            // To achieve this we have to pull the camera towards the positive z axis
            // based on the following formula:
            // z = (desired_height / 2) / tan(fovy/2)
            val z =
                (desiredHeight / 2 / tan(fovy / 2 * (Math.PI / 180.0f))).toFloat()
            // forward for the camera is backward for the scene
            gl.glTranslatef(0.0f, 0.0f, -z)
            // rotate local to achieve top down view from negative y down to xz-plane
            // z range is the desired height
            gl.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f)
            // save local system as a basis to draw scene items
            gl11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, modelViewScene, 0)
            // window boundaries
            boundaries.updateWith(desiredHeight, aspectRatio)
            // touch event pixel coordinates to openGL coordinates
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
