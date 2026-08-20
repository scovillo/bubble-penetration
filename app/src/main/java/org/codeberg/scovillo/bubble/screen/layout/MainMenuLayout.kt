package org.codeberg.scovillo.bubble.screen.layout

import android.graphics.Typeface
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import org.codeberg.scovillo.bubble.MainActivity
import org.codeberg.scovillo.bubble.R
import org.codeberg.scovillo.bubble.THREAD_POOL
import org.codeberg.scovillo.bubble.data.ApiService
import org.codeberg.scovillo.bubble.screen.MenuGLSurfaceView
import org.codeberg.scovillo.bubble.sound.MusicPlayer
import java.util.concurrent.TimeUnit

class MainMenuLayout(private val mainActivity: MainActivity, private val musicPlayer: MusicPlayer) {

    var menuGLSurfaceView: MenuGLSurfaceView? = null

    fun show() {
        mainActivity.setContentView(R.layout.activity_main)

        loadCurrentChampionAsync()

        menuGLSurfaceView = MenuGLSurfaceView(mainActivity)
        val championTextView = mainActivity.findViewById<TextView?>(R.id.champion_text)
        championTextView?.typeface = Typeface.createFromAsset(
            mainActivity.assets,
            "fonts/PLUMP.ttf"
        )
        val glSurfaceViewHolder = mainActivity.findViewById<View>(R.id.menuGLSurfaceViewHolder) as FrameLayout
        glSurfaceViewHolder.addView(menuGLSurfaceView)
        (mainActivity.findViewById<View>(R.id.menu_username) as TextView).text = mainActivity.selectedUser.name
        (mainActivity.findViewById<View>(R.id.effects_box) as CheckBox).typeface = Typeface.createFromAsset(
            mainActivity.assets,
            "fonts/PLUMP.ttf"
        )
        (mainActivity.findViewById<View>(R.id.music_box) as CheckBox).typeface = Typeface.createFromAsset(
            mainActivity.assets,
            "fonts/PLUMP.ttf"
        )
        (mainActivity.findViewById<View>(R.id.menu_title) as TextView).typeface = Typeface.createFromAsset(
            mainActivity.assets,
            "fonts/PLUMP.ttf"
        )
        (mainActivity.findViewById<View>(R.id.menu_username) as TextView).typeface = Typeface.createFromAsset(
            mainActivity.assets,
            "fonts/PLUMP.ttf"
        )
        (mainActivity.findViewById<View>(R.id.start_button) as Button).typeface = Typeface.createFromAsset(
            mainActivity.assets,
            "fonts/PLUMP.ttf"
        )
        (mainActivity.findViewById<View>(R.id.highscore_button) as Button).typeface = Typeface.createFromAsset(
            mainActivity.assets,
            "fonts/PLUMP.ttf"
        )

        (mainActivity.findViewById<View>(R.id.music_box) as CheckBox).isChecked = !mainActivity.settingsModel.isMusicMuted
        (mainActivity.findViewById<View>(R.id.effects_box) as CheckBox).isChecked = !mainActivity.settingsModel.areSoundEffectsMuted
        
        val anim: Animation = AlphaAnimation(0.35f, 1.0f)
        anim.duration = 300
        anim.startOffset = 20
        anim.repeatMode = Animation.REVERSE
        anim.repeatCount = Animation.INFINITE
        val title = mainActivity.findViewById<View>(R.id.menu_title) as TextView
        title.startAnimation(anim)
    }

    fun hide() {
        val glSurfaceViewHolder = mainActivity.findViewById<View>(R.id.menuGLSurfaceViewHolder) as FrameLayout
        glSurfaceViewHolder.removeAllViews()
    }

    private fun loadCurrentChampionAsync() {
        THREAD_POOL.execute {
            try {
                val highscoreRequest = ApiService.getHighscoreData()
                val jsonArray = highscoreRequest[8000, TimeUnit.MILLISECONDS]

                mainActivity.runOnUiThread {
                    val championTextView = mainActivity.findViewById<TextView?>(R.id.champion_text)
                        ?: return@runOnUiThread
                    if (jsonArray.length() > 0) {
                        championTextView.text = mainActivity.getString(
                            R.string.champion_label,
                            jsonArray.getJSONObject(0).getString("name"),
                            jsonArray.getJSONObject(0).getString("score")
                        )
                    } else {
                        championTextView.text = mainActivity.getString(R.string.no_champion)
                    }
                }
            } catch (exception: Exception) {
                mainActivity.runOnUiThread {
                    val championTextView = mainActivity.findViewById<TextView?>(R.id.champion_text)
                        ?: return@runOnUiThread
                    Toast.makeText(mainActivity, mainActivity.getString(R.string.server_unavailable), Toast.LENGTH_LONG).show()
                    championTextView.text = mainActivity.getString(R.string.no_champion)
                }
            }
        }
    }

}
