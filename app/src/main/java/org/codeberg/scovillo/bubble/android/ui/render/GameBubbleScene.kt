package org.codeberg.scovillo.bubble.android.ui.render

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import kotlinx.coroutines.runBlocking
import org.codeberg.scovillo.bubble.R
import org.codeberg.scovillo.bubble.android.MainActivity
import org.codeberg.scovillo.bubble.android.api.OnlineGameSession
import org.codeberg.scovillo.bubble.android.game.rgb
import org.codeberg.scovillo.bubble.android.persistence.MatchSettings
import org.codeberg.scovillo.bubble.android.ui.hud.ScorePostfix
import org.codeberg.scovillo.bubble.android.ui.hud.TimerPostfix
import org.codeberg.scovillo.bubble.game.GameActionEvent
import org.codeberg.scovillo.bubble.game.GameObjectColor
import org.codeberg.scovillo.bubble.game.engine.MatchEngine
import org.codeberg.scovillo.bubble.game.engine.MatchEvent
import org.codeberg.scovillo.bubble.game.engine.MatchSnapshot
import java.math.RoundingMode
import java.text.DecimalFormat
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GameBubbleScene(
    private val mainActivity: MainActivity,
    private val matchSettings: MatchSettings,
    private val engine: MatchEngine,
    private val session: OnlineGameSession?
) : BubbleScene {

    private val effectPlayer = mainActivity.soundEffects

    private val openGlScene = OpenGlScene(engine.boundaries, SceneLighting.GAME)

    @Volatile
    private var gameOverQueued = false
    private val timerText: TextView = mainActivity.findViewById<View>(R.id.Timer) as TextView
    private val scoreText: TextView = mainActivity.findViewById<View>(R.id.Score) as TextView
    private val comboText: TextView = mainActivity.findViewById<View>(R.id.Combo) as TextView
    private val timerProgress: ProgressBar = mainActivity.findViewById(R.id.TimerProgress)
    private val timerPill: View = mainActivity.findViewById(R.id.TimerPill)
    private val fieldHolder: View = mainActivity.findViewById(R.id.GLSurfaceViewHolder)
    private val bubbleRenderer = BubbleRenderer()
    override val renderer: GLSurfaceView.Renderer = bubbleRenderer
    private val comboPulseMinAlpha = 0.55f
    private val comboPulseTransitionDurationMs = 180L
    private var fieldFrameAlpha = 255
    private val fieldFrameDrawable = GradientDrawable().apply {
        cornerRadius = 10f * mainActivity.resources.displayMetrics.density
        alpha = fieldFrameAlpha
    }
    private var fieldFramePulseAnimator: ValueAnimator? = null
    private var fieldFramePulseGeneration = 0
    private val firstDigitFormat = DecimalFormat("0.0")

    init {

        effectPlayer.isMuted = matchSettings.areSoundEffectsMuted
        firstDigitFormat.roundingMode = RoundingMode.CEILING
        ViewCompat.setBackground(fieldHolder, fieldFrameDrawable)

    }

    override fun onResume() {
        openGlScene.resetFrameTime()
    }

    fun getScore(): Int = engine.score
    fun getTimer(): Float = engine.timerSeconds
    fun getActionLog(): List<GameActionEvent> = engine.log
    fun getDurationMs(): Long = engine.durationMs
    fun getViewportAspectRatio(): Float = engine.viewportAspectRatio

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (engine.isGameOver) return false
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                val surfaceWidth = bubbleRenderer.surfaceWidth
                val surfaceHeight = bubbleRenderer.surfaceHeight
                if (surfaceWidth <= 0 || surfaceHeight <= 0) {
                    return false
                }
                val normalizedX = engine.roundCoordinate(
                    (event.x / surfaceWidth).toDouble().coerceIn(0.0, 1.0)
                )
                val normalizedY = engine.roundCoordinate(
                    (event.y / surfaceHeight).toDouble().coerceIn(0.0, 1.0)
                )
                val wasBubbleTouched = engine.submitTap(normalizedX, normalizedY)
                return wasBubbleTouched
            }
        }
        return false
    }

    private inner class BubbleRenderer : GLSurfaceView.Renderer {

        var unitsPerPixelX = 0f
            private set
        var unitsPerPixelZ = 0f
            private set
        var surfaceWidth = 0
            private set
        var surfaceHeight = 0
            private set

        private val timerTextAnimation: Animation = AlphaAnimation(0.35f, 1.0f)
        private val timerPostfix = TimerPostfix(mainActivity)
        private val scorePostfix = ScorePostfix(mainActivity)
        private var isTimerProgressUrgent = false
        private var shownCollectColor = GameObjectColor.RED
        private var isCollectColorShown = false

        init {
            timerTextAnimation.duration = 300
            timerTextAnimation.startOffset = 20
            timerTextAnimation.repeatMode = Animation.REVERSE
            timerTextAnimation.repeatCount = Animation.INFINITE
        }

        override fun onDrawFrame(gl: GL10) {
            if (gameOverQueued) {
                openGlScene.draw(gl, engine.gameObjects)
                return
            }
            val fracSec = openGlScene.elapsedSeconds()
            runBlocking { engine.advance(fracSec, ::handleEngineEvent) }
            openGlScene.draw(gl, engine.gameObjects)
        }

        fun handleEngineEvent(event: MatchEvent) {
            when (event) {
                is MatchEvent.StateChanged -> renderHud(event.snapshot)
                MatchEvent.TimerAlarm -> effectPlayer.playSound(R.raw.alarm)
                is MatchEvent.TargetCollected -> {
                    timerPostfix.animateWith(event.timerDeltaSeconds)
                    if (event.scoreDelta != 0) scorePostfix.animateWith(event.scoreDelta)
                    effectPlayer.playSound(
                        when {
                            event.isStar -> R.raw.star
                            event.wasCorrectColor -> R.raw.blubb
                            else -> R.raw.fart
                        }
                    )
                    if (event.comboMultiplierIncreased) {
                        effectPlayer.playSound(
                            when (event.comboIsActive) {
                                true -> R.raw.combo
                                false -> R.raw.blubb
                            }
                        )
                    }
                    // Match events are emitted from the OpenGL render thread. ValueAnimator
                    // requires a Looper, so creating the frame animation there crashes as
                    // soon as the first target is collected.
                    mainActivity.runOnUiThread {
                        transitionFieldFramePulse(
                            when (event.comboIsActive) {
                                true -> 900L
                                false -> null
                            }
                        )
                    }
                }

                MatchEvent.GameOver -> {
                    gameOverQueued = true
                    mainActivity.runOnUiThread {
                        mainActivity.showGameOverScreen(engine.score.toString(), session)
                    }
                }
            }
        }

        private fun renderHud(snapshot: MatchSnapshot) = mainActivity.runOnUiThread {
            when {
                snapshot.timerSeconds < 10 -> {
                    if (timerText.animation == null) {
                        timerText.startAnimation(timerTextAnimation)
                    }
                }

                snapshot.timerSeconds >= 10 -> {
                    timerText.animation?.cancel()
                }
            }
            timerText.text =
                mainActivity.getString(
                    R.string.timer_value,
                    firstDigitFormat.format(snapshot.timerSeconds)
                )
            scoreText.text = mainActivity.getString(R.string.score_value, snapshot.score)
            renderCombo(snapshot)
            if (!isCollectColorShown || shownCollectColor != snapshot.collectColor) {
                shownCollectColor = snapshot.collectColor
                isCollectColorShown = true
                updateCollectColorIndicator(snapshot.collectColor.rgb())
            }
            timerProgress.progress =
                (snapshot.timerSeconds / snapshot.initialTimerSeconds * 1000).toInt()
                    .coerceIn(0, 1000)
            val shouldShowUrgentProgress = snapshot.timerSeconds < 10
            if (shouldShowUrgentProgress != isTimerProgressUrgent) {
                isTimerProgressUrgent = shouldShowUrgentProgress
                timerProgress.progressDrawable = ContextCompat.getDrawable(
                    mainActivity,
                    if (isTimerProgressUrgent) R.drawable.timer_progress_urgent else R.drawable.timer_progress
                )
            }
        }

        private fun renderCombo(snapshot: MatchSnapshot) {
            if (snapshot.comboProgress == 0) {
                comboText.visibility = View.INVISIBLE
                return
            }
            val filledSteps = snapshot.comboProgress % 6
            val label = when (snapshot.comboMultiplier) {
                1 -> mainActivity.getString(R.string.combo_build)
                2 -> mainActivity.getString(R.string.combo)
                4 -> mainActivity.getString(R.string.combo_hot_streak)
                8 -> mainActivity.getString(R.string.combo_on_fire)
                else -> mainActivity.getString(R.string.combo_mega)
            }
            comboText.text =
                "$label ×${snapshot.comboMultiplier}  ${"●".repeat(filledSteps)}${"○".repeat(6 - filledSteps)}"
            comboText.visibility = View.VISIBLE
        }

        override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
            val dimensions = openGlScene.resize(gl, width, height)
            surfaceWidth = width
            surfaceHeight = height
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
                setStroke(
                    (2f * mainActivity.resources.displayMetrics.density).toInt(),
                    Color.rgb(red, green, blue)
                )
            })
            ViewCompat.setBackground(scoreText, GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18f * mainActivity.resources.displayMetrics.density
                setColor(Color.rgb(18, 24, 51))
                setStroke(
                    (2f * mainActivity.resources.displayMetrics.density).toInt(),
                    Color.rgb(red, green, blue)
                )
            })
            fieldFrameDrawable.setColor(Color.rgb(red, green, blue))
        }

        fun transitionFieldFramePulse(duration: Long?) {
            val generation = ++fieldFramePulseGeneration
            val currentAlpha = fieldFrameAlpha
            fieldFramePulseAnimator?.cancel()
            fieldFramePulseAnimator = ValueAnimator.ofInt(currentAlpha, 255).apply {
                this.duration = comboPulseTransitionDurationMs
                addUpdateListener { animator ->
                    setFieldFrameAlpha(animator.animatedValue as Int)
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (generation == fieldFramePulseGeneration && duration != null) {
                            startFieldFramePulse(duration)
                        }
                    }
                })
                start()
            }
        }

        private fun startFieldFramePulse(duration: Long) {
            fieldFramePulseAnimator =
                ValueAnimator.ofInt(255, (255 * comboPulseMinAlpha).toInt()).apply {
                    this.duration = duration
                    repeatMode = ValueAnimator.REVERSE
                    repeatCount = ValueAnimator.INFINITE
                    addUpdateListener { animator ->
                        setFieldFrameAlpha(animator.animatedValue as Int)
                    }
                    start()
                }
        }

        private fun setFieldFrameAlpha(alpha: Int) {
            fieldFrameAlpha = alpha
            fieldFrameDrawable.alpha = alpha
        }

    }

}
