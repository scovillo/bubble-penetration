package org.codeberg.scovillo.bubble.screen.layout

import android.graphics.Typeface
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import org.codeberg.scovillo.bubble.MainActivity
import org.codeberg.scovillo.bubble.R
import org.codeberg.scovillo.bubble.data.DataConnection
import org.codeberg.scovillo.bubble.screen.MenuGLSurfaceView
import org.codeberg.scovillo.bubble.sound.MusicPlayer
import org.codeberg.scovillo.bubble.THREAD_POOL
import java.util.concurrent.TimeUnit

class MainMenuLayout(private val mainActivity: MainActivity, private val musicPlayer: MusicPlayer) {

    var menuGLSurfaceView: MenuGLSurfaceView? = null

    fun show() {
        mainActivity.setContentView(R.layout.activity_main)

        loadCurrentChampionAsync()

        menuGLSurfaceView = MenuGLSurfaceView(mainActivity)
        val championTextView = (mainActivity.findViewById<View>(R.id.champion_text) as TextView)
        championTextView.typeface = Typeface.createFromAsset(
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
        if (musicPlayer.isMuted) {
            (mainActivity.findViewById<View>(R.id.music_box) as CheckBox).isChecked = false
        }
        if (mainActivity.areSoundEffectsMuted) {
            (mainActivity.findViewById<View>(R.id.effects_box) as CheckBox).isChecked = false
        }
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
            val championTextView = (mainActivity.findViewById<View>(R.id.champion_text) as TextView)
            try {
                val highscoreRequest = DataConnection.getHighscoreData()
                val jsonArray = highscoreRequest[8000, TimeUnit.MILLISECONDS]

                mainActivity.runOnUiThread {
                    if (jsonArray.length() > 0) {
                        championTextView.text = "Champion:\n${jsonArray.getJSONObject(0).getString("name")} with ${jsonArray.getJSONObject(0).getString("score")} !"
                    } else {
                        championTextView.text = mainActivity.getString(R.string.NO_CHAMPION)
                    }
                }
            } catch (exception: Exception) {
                mainActivity.runOnUiThread {
                    Toast.makeText(mainActivity, "Server is currently not available...please try again later.", Toast.LENGTH_LONG).show()
                    championTextView.text = mainActivity.getString(R.string.NO_CHAMPION)
                }
            }
        }
    }

}