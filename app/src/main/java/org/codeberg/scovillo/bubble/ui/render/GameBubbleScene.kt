package org.codeberg.scovillo.bubble.ui.render

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
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
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.pow
import kotlin.math.sqrt

class GameBubbleScene(
    private val mainActivity: MainActivity,
    initialScore: Int,
    initialTimer: Float,
    areSoundEffectsMuted: Boolean,
) : BubbleScene {

    private val effectPlayer = mainActivity.soundEffects

    private val timerAlarm = TimerAlarm()
    private val boundaries = Boundaries()
    private val openGlScene = OpenGlScene(boundaries, SceneLighting.GAME)
    private val gameObjects = ArrayList<GameObject>()
    private val generator = Generator(gameObjects, boundaries)
    private var collectColor = BubbleColors.RED
    private var timer = initialTimer
    private var score = initialScore
    private var isTouch = false
    private val objectsToBeRemoved = ArrayList<GameObject>()
    private val targetsToBeRemoved = ArrayList<GameObject>()
    private val timerText: TextView = mainActivity.findViewById<View>(R.id.Timer) as TextView
    private val scoreText: TextView = mainActivity.findViewById<View>(R.id.Score) as TextView
    private val timerProgress: ProgressBar = mainActivity.findViewById(R.id.TimerProgress)
    private val timerPill: View = mainActivity.findViewById(R.id.TimerPill)
    private val fieldHolder: View = mainActivity.findViewById(R.id.GLSurfaceViewHolder)
    private val bubbleRenderer = BubbleRenderer()
    override val renderer: GLSurfaceView.Renderer = bubbleRenderer
    private val timeLogic = Time()
    private val combo = Combo(mainActivity, effectPlayer)
    private val firstDigitFormat = DecimalFormat("0.0")

    init {

        effectPlayer.isMuted = areSoundEffectsMuted
        firstDigitFormat.roundingMode = RoundingMode.CEILING

    }

    override fun onResume() {
        openGlScene.resetFrameTime()
    }

    fun getScore(): Int = score
    fun getTimer(): Float = timer

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var wasBubbleTouched = false
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                isTouch = true
                var targetIndex = -1
                var targetCounter = 0
                var distance = 0.0
                var i = 0
                while (i < gameObjects.size) {
                    val bubble = gameObjects[i]
                    val x = event.x * bubbleRenderer.unitsPerPixelX - boundaries.right - bubble.x
                    val y = (event.y * bubbleRenderer.unitsPerPixelZ - boundaries.top) * -1 - bubble.z
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
                    wasBubbleTouched = true
                }
                isTouch = false
            }
        }
        return wasBubbleTouched
    }

    private inner class BubbleRenderer : GLSurfaceView.Renderer {

        var unitsPerPixelX = 0f
            private set
        var unitsPerPixelZ = 0f
            private set

        private val timerTextAnimation: Animation = AlphaAnimation(0.35f, 1.0f)
        private val timerPostfix = TimerPostfix(mainActivity)
        private val scorePostfix = ScorePostfix(mainActivity)
        private var isTimerProgressUrgent = false
        private var shownCollectColor = BubbleColors.RED
        private var isCollectColorShown = false

        init {
            timerTextAnimation.duration = 300
            timerTextAnimation.startOffset = 20
            timerTextAnimation.repeatMode = Animation.REVERSE
            timerTextAnimation.repeatCount = Animation.INFINITE
        }

        override fun onDrawFrame(gl: GL10) {
            val fracSec = openGlScene.elapsedSeconds()
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
                    mainActivity.getString(R.string.timer_value, firstDigitFormat.format(timer))
                scoreText.text = mainActivity.getString(R.string.score_value, score)
                if (!isCollectColorShown || shownCollectColor != collectColor) {
                    shownCollectColor = collectColor
                    isCollectColorShown = true
                    val targetColor = generator.getGLColor(collectColor)
                    updateCollectColorIndicator(targetColor)
                }
                timerProgress.progress = (timer / INITIAL_TIMER * 1000).toInt().coerceIn(0, 1000)
                val shouldShowUrgentProgress = timer < 10
                if (shouldShowUrgentProgress != isTimerProgressUrgent) {
                    isTimerProgressUrgent = shouldShowUrgentProgress
                    timerProgress.progressDrawable = ContextCompat.getDrawable(
                        mainActivity,
                        if (isTimerProgressUrgent) R.drawable.timer_progress_urgent else R.drawable.timer_progress
                    )
                }
            }
            updateGameObjects(fracSec)
            combo.update()
            openGlScene.draw(gl, gameObjects)
        }

        private fun updateGameObjects(fracSec: Float) {

            gameObjects.forEach {
                it.update(fracSec)
                if (it.isOutside(boundaries)) {
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
            val dimensions = openGlScene.resize(gl, width, height)
            unitsPerPixelZ = dimensions.height / height
            unitsPerPixelX = dimensions.height * dimensions.aspectRatio / width
        }

        override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
            openGlScene.initialize(gl)
        }

        private fun updateCollectColorIndicator(color: FloatArray) {
            val red = (color[0] * 255).toInt()
            val green = (color[1] * 255).toInt()
            val blue = (color[2] * 255).toInt()
            ViewCompat.setBackground(timerPill, GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18f * mainActivity.resources.displayMetrics.density
                setColor(Color.rgb(18, 24, 51))
                setStroke((2f * mainActivity.resources.displayMetrics.density).toInt(), Color.rgb(red, green, blue))
            })
            ViewCompat.setBackground(scoreText, GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18f * mainActivity.resources.displayMetrics.density
                setColor(Color.rgb(18, 24, 51))
                setStroke((2f * mainActivity.resources.displayMetrics.density).toInt(), Color.rgb(red, green, blue))
            })
            ViewCompat.setBackground(fieldHolder, GradientDrawable().apply {
                setColor(Color.rgb(red, green, blue))
                cornerRadius = 10f * mainActivity.resources.displayMetrics.density
            })
        }

    }

    companion object {
        private const val INITIAL_TIMER = 25.0f
    }

}
