package de.spicysources.bubblepenetration.screen.layout

import android.graphics.Typeface
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.TextView
import de.spicysources.bubblepenetration.MainActivity
import de.spicysources.bubblepenetration.R
import de.spicysources.bubblepenetration.screen.MenuGLSurfaceView
import de.spicysources.bubblepenetration.sound.MusicPlayer

class MainMenuLayout(private val mainActivity: MainActivity, private val musicPlayer: MusicPlayer) {

    private var menuGLSurfaceView: MenuGLSurfaceView? = null

    fun show() {
        mainActivity.setContentView(R.layout.activity_main)
        menuGLSurfaceView = MenuGLSurfaceView(mainActivity)
        val glSurfaceViewHolder = mainActivity.findViewById<View>(R.id.menuGLSurfaceViewHolder) as FrameLayout
        glSurfaceViewHolder.addView(menuGLSurfaceView)
        (mainActivity.findViewById<View>(R.id.menu_username) as TextView).text = mainActivity.username
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
    }

    fun hide() {
        val glSurfaceViewHolder = mainActivity.findViewById<View>(R.id.menuGLSurfaceViewHolder) as FrameLayout
        glSurfaceViewHolder.removeAllViews()
    }

}