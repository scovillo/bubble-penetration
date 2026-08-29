package org.codeberg.scovillo.bubble.ui.layout

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import org.codeberg.scovillo.bubble.MainActivity
import org.codeberg.scovillo.bubble.R
import org.codeberg.scovillo.bubble.THREAD_POOL
import org.codeberg.scovillo.bubble.api.ApiService
import org.codeberg.scovillo.bubble.sound.MusicPlayer
import org.codeberg.scovillo.bubble.ui.render.BubbleGLSurfaceView
import org.codeberg.scovillo.bubble.ui.render.MenuBubbleScene
import java.util.concurrent.TimeUnit

class MainMenuLayout(private val mainActivity: MainActivity, private val musicPlayer: MusicPlayer) {

    private var startButtonPulse: AnimatorSet? = null

    fun show() {
        mainActivity.setContentView(R.layout.activity_main)

        loadCurrentChampionAsync()

        val menuGLSurfaceView = BubbleGLSurfaceView(mainActivity, MenuBubbleScene())
        val glSurfaceViewHolder = mainActivity.findViewById<View>(R.id.menuGLSurfaceViewHolder) as FrameLayout
        glSurfaceViewHolder.addView(menuGLSurfaceView)
        (mainActivity.findViewById<View>(R.id.menu_username) as TextView).text = mainActivity.selectedUser.username
        val startButton = mainActivity.findViewById<View>(R.id.start_button) as Button
        startButtonPulse?.cancel()
        startButtonPulse = createStartButtonPulse(startButton).also { it.start() }

        val anim: Animation = AlphaAnimation(0.35f, 1.0f)
        anim.duration = 300
        anim.startOffset = 20
        anim.repeatMode = Animation.REVERSE
        anim.repeatCount = Animation.INFINITE
        val title = mainActivity.findViewById<View>(R.id.menu_title) as TextView
        title.startAnimation(anim)
    }

    fun hide() {
        startButtonPulse?.cancel()
        startButtonPulse = null
        val glSurfaceViewHolder = mainActivity.findViewById<View?>(R.id.menuGLSurfaceViewHolder) as? FrameLayout ?: return
        glSurfaceViewHolder.removeAllViews()
    }

    private fun createStartButtonPulse(button: Button): AnimatorSet {
        fun pulse(property: String, from: Float, to: Float) = ObjectAnimator.ofFloat(button, property, from, to).apply {
            duration = 900
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }

        return AnimatorSet().apply {
            startDelay = 250
            playTogether(
                pulse("scaleX", 1.0f, 1.075f),
                pulse("scaleY", 1.0f, 1.075f),
                pulse("alpha", 0.9f, 1.0f)
            )
        }
    }

    private fun loadCurrentChampionAsync() {
        if (!mainActivity.settingsModel.useOnlineLeaderboard) {
            showLocalChampion()
            return
        }
        THREAD_POOL.execute {
            try {
                val highscoreRequest = ApiService.getHighscorePage(startRank = 1)
                val jsonArray = highscoreRequest[8000, TimeUnit.MILLISECONDS].getJSONArray("highscores")
                mainActivity.onBackendRequestSucceeded()

                mainActivity.runOnUiThread {
                    val championTextView = mainActivity.findViewById<TextView?>(R.id.champion_text)
                        ?: return@runOnUiThread
                    if (jsonArray.length() > 0) {
                        championTextView.text = championLabel(
                            jsonArray.getJSONObject(0).getString("username"),
                            jsonArray.getJSONObject(0).getString("score"),
                        )
                    } else {
                        championTextView.text = mainActivity.getString(R.string.no_champion)
                    }
                }
            } catch (exception: Exception) {
                mainActivity.runOnUiThread {
                    showLocalChampion()
                    if (!mainActivity.showRateLimitMessage(exception)) {
                        mainActivity.showOfflineFallbackMessageOnce()
                    }
                }
                exception.printStackTrace()
            }
        }
    }

    private fun showLocalChampion() {
        val champion = mainActivity.localHighscoreStorage.read().firstOrNull()
        val championTextView = mainActivity.findViewById<TextView?>(R.id.champion_text) ?: return
        championTextView.text = champion?.let {
            championLabel(it.username, it.score.toString())
        } ?: mainActivity.getString(R.string.no_champion)
    }

    private fun championLabel(username: String, score: String): CharSequence {
        val label = mainActivity.getString(R.string.champion_label, username, score)
        return SpannableString(label).apply {
            setBold(username, label.indexOf(username))
            setBold(score, label.lastIndexOf(score))
        }
    }

    private fun SpannableString.setBold(value: String, start: Int) {
        if (value.isNotEmpty() && start >= 0) {
            setSpan(StyleSpan(Typeface.BOLD), start, start + value.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

}
