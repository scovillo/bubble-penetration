package org.codeberg.scovillo.bubble.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import android.widget.ProgressBar
import org.codeberg.scovillo.bubble.MainActivity
import org.codeberg.scovillo.bubble.R
import org.codeberg.scovillo.bubble.game.Bubble
import org.codeberg.scovillo.bubble.game.BubbleColors
import org.codeberg.scovillo.bubble.game.Combo
import org.codeberg.scovillo.bubble.game.GameObject
import org.codeberg.scovillo.bubble.game.Generator
import org.codeberg.scovillo.bubble.game.Star
import org.codeberg.scovillo.bubble.game.Time
import org.codeberg.scovillo.bubble.game.TimerAlarm
import org.codeberg.scovillo.bubble.ui.hud.ScorePostfix
import org.codeberg.scovillo.bubble.ui.hud.TimerPostfix
import java.math.RoundingMode
import java.text.DecimalFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.opengles.GL11
import kotlin.math.pow
import kotlin.math.sqrt

class BubbleGLSurfaceView(context: Context) : GLSurfaceView(context) {

    private val mainActivity = context as MainActivity
    private val effectPlayer = mainActivity.soundEffects

    private val timerAlarm = TimerAlarm()
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
    private var scoreText: TextView = mainActivity.findViewById<View>(R.id.Score) as TextView
    private val timerProgress: ProgressBar = mainActivity.findViewById(R.id.TimerProgress)
    private val timerPill: View = mainActivity.findViewById(R.id.TimerPill)
    private val fieldHolder: View = mainActivity.findViewById(R.id.GLSurfaceViewHolder)
    private val renderer: BubbleRenderer
    private val timeLogic = Time()
    private val combo = Combo(mainActivity, effectPlayer)
    private val firstDigitFormat = DecimalFormat("0.0")

    init {

        firstDigitFormat.roundingMode = RoundingMode.CEILING

        renderer = BubbleRenderer()
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
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
        private var isTimerProgressUrgent = false
        private var shownCollectColor: BubbleColors? = null

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
            if (timerAlarm.shouldTrigger(timer, SystemClock.elapsedRealtime())) {
                effectPlayer.playSound(R.raw.alarm)
            }
            if (timer - fracSec <= 0.0) {
                timer = 0.0f
            } else {
                timer -= fracSec
            }
            collectColor = generator.generateCollectColor(collectColor, score)
            combo.setTargetColor(generator.getGLColor(collectColor))
            mainActivity.runOnUiThread {
                when {
                    timer <= 0.0 -> {
                        mainActivity.showGameOverScreenWith(score.toString())
                        return@runOnUiThread
                    }

                    timer < 10 -> {
                        if (timerText.animation == null) {
                            timerText.startAnimation(timerTextAnimation)
                        }
                    }

                    timer >= 10 -> {
                        timerText.animation?.cancel()
                    }
                }
                timerText.text =
                    context.getString(R.string.timer_value, firstDigitFormat.format(timer))
                scoreText.text = context.getString(R.string.score_value, score)
                if (shownCollectColor != collectColor) {
                    shownCollectColor = collectColor
                    val targetColor = generator.getGLColor(collectColor)
                    updateCollectColorIndicator(targetColor)
                }
                timerProgress.progress = (timer / INITIAL_TIMER * 1000).toInt().coerceIn(0, 1000)
                val shouldShowUrgentProgress = timer < 10
                if (shouldShowUrgentProgress != isTimerProgressUrgent) {
                    isTimerProgressUrgent = shouldShowUrgentProgress
                    timerProgress.progressDrawable = mainActivity.getDrawable(
                        if (isTimerProgressUrgent) R.drawable.timer_progress_urgent else R.drawable.timer_progress
                    )
                }
            }
            updateGameObjects(fracSec)
            combo.update()
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)
            val gl11 = gl as GL11
            gl.glMatrixMode(GL10.GL_MODELVIEW)
            gl11.glLoadMatrixf(modelViewScene, 0)
            gameObjects.forEach { it.draw(gl) }
            drawFieldFrame(gl)
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

                        if (combo.isActive) {
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
            gl.glMatrixMode(GL10.GL_PROJECTION)
            gl.glLoadIdentity()
            val desiredHeight = if (aspectRatio > 1.0f) 10.0f else 10.0f / aspectRatio
            val desiredWidth = desiredHeight * aspectRatio
            gl.glOrthof(
                -desiredWidth / 2,
                desiredWidth / 2,
                -desiredHeight / 2,
                desiredHeight / 2,
                0.001f,
                100.0f,
            )
            gl.glMatrixMode(GL10.GL_MODELVIEW)
            gl.glLoadIdentity()
            gl.glTranslatef(0.0f, 0.0f, -20.0f)
            // rotate local to achieve top down view from negative y down to xz-plane
            // z range is the desired height
            gl.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f)
            // save local system as a basis to draw scene items
            gl11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, modelViewScene, 0)
            // window boundaries
            boundaries.updateWith(desiredHeight, aspectRatio)
            updateFieldFrame()
            // touch event pixel coordinates to openGL coordinates
            unitsPerPixelZ = desiredHeight / height
            unitsPerPixelX = desiredHeight * aspectRatio / width
        }

        override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
            gl.glDisable(GL10.GL_DITHER)
            gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST)
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
            gl.glEnable(GL10.GL_BLEND)
            gl.glClearColor(0.027f, 0.035f, 0.11f, 1.0f)
            gl.glEnable(GL10.GL_CULL_FACE)
            gl.glShadeModel(GL10.GL_FLAT)
            gl.glEnable(GL10.GL_DEPTH_TEST)
            gl.glDepthFunc(GL10.GL_LEQUAL)
            gl.glShadeModel(GL10.GL_SMOOTH)
            gl.glEnable(GL10.GL_DEPTH_TEST)
            gl.glEnable(GL10.GL_NORMALIZE)
            gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, LIGHT_AMBIENT, 0)
            gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, LIGHT_DIFFUSE, 0)
            gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPECULAR, LIGHT_SPECULAR, 0)
            gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, LIGHT_POSITION, 0)
            gl.glEnable(GL10.GL_LIGHT0)
        }

        private fun updateFieldFrame() {
            val inset = 0.06f
            fieldFrameBuffer.clear()
            fieldFrameBuffer.put(boundaries.left + inset).put(0f).put(boundaries.bottom + inset)
            fieldFrameBuffer.put(boundaries.right - inset).put(0f).put(boundaries.bottom + inset)
            fieldFrameBuffer.put(boundaries.right - inset).put(0f).put(boundaries.top - inset)
            fieldFrameBuffer.put(boundaries.left + inset).put(0f).put(boundaries.top - inset)
            fieldFrameBuffer.position(0)
        }

        private fun drawFieldFrame(gl: GL10) {
            gl.glDisable(GL10.GL_DEPTH_TEST)
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY)
            fieldFrameBuffer.position(0)
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, fieldFrameBuffer)
            gl.glColor4f(0.30f, 0.48f, 0.86f, 0.34f)
            gl.glLineWidth(1.5f)
            gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, 4)
            gl.glDisableClientState(GL10.GL_VERTEX_ARRAY)
            gl.glEnable(GL10.GL_DEPTH_TEST)
        }

        private fun updateCollectColorIndicator(color: FloatArray) {
            val red = (color[0] * 255).toInt()
            val green = (color[1] * 255).toInt()
            val blue = (color[2] * 255).toInt()
            timerPill.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18f * resources.displayMetrics.density
                setColor(Color.rgb(18, 24, 51))
                setStroke((2f * resources.displayMetrics.density).toInt(), Color.rgb(red, green, blue))
            }
            scoreText.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18f * resources.displayMetrics.density
                setColor(Color.rgb(18, 24, 51))
                setStroke((2f * resources.displayMetrics.density).toInt(), Color.rgb(red, green, blue))
            }
            // The outer arena frame is the primary, at-a-glance target-colour indicator.
            fieldHolder.background = GradientDrawable().apply {
                setColor(Color.rgb(red, green, blue))
                cornerRadius = 10f * resources.displayMetrics.density
            }
        }

    }

    private val fieldFrameBuffer: FloatBuffer = ByteBuffer.allocateDirect(12 * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder()).asFloatBuffer()

    companion object {
        private const val INITIAL_TIMER = 25.0f
        private val LIGHT_AMBIENT = floatArrayOf(0.20f, 0.20f, 0.30f, 1.0f)
        private val LIGHT_DIFFUSE = floatArrayOf(0.72f, 0.76f, 0.88f, 1.0f)
        private val LIGHT_SPECULAR = floatArrayOf(0.76f, 0.80f, 0.92f, 1.0f)
        private val LIGHT_POSITION = floatArrayOf(-4.0f, 8.0f, 6.0f, 1.0f)
    }

}
