package de.spicysources.bubblepenetration

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.*
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT
import de.spicysources.bubblepenetration.data.DataConnection
import de.spicysources.bubblepenetration.data.LocalFileStorage
import de.spicysources.bubblepenetration.screen.BubbleGLSurfaceView
import de.spicysources.bubblepenetration.screen.layout.GameOverScreenLayout
import de.spicysources.bubblepenetration.screen.layout.HighscoreLayout
import de.spicysources.bubblepenetration.screen.layout.MainMenuLayout
import de.spicysources.bubblepenetration.screen.layout.UsernameLayout
import de.spicysources.bubblepenetration.sound.MusicPlayer
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class MainActivity : Activity() {

    var areSoundEffectsMuted = false
        private set

    private val musicPlayer = MusicPlayer(this)
    private val localFileStorage = LocalFileStorage(this)

    private val gameOverScreenLayout = GameOverScreenLayout(this)
    private val usernameLayout = UsernameLayout(this)
    private val highscoreLayout = HighscoreLayout(this)
    private val mainMenuLayout = MainMenuLayout(this, musicPlayer)

    lateinit var username: String
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        musicPlayer.init()
        username = localFileStorage.readFromFile()
        if (username.isBlank()) {
            usernameLayout.showWith(false)
        } else {
            mainMenuLayout.show()
        }
    }

    public override fun onResume() {
        super.onResume()
        musicPlayer.start()
    }

    public override fun onPause() {
        super.onPause()
        musicPlayer.pause()
    }

    fun startGame(view: View) {
        mainMenuLayout.hide()
        areSoundEffectsMuted = !(findViewById<View>(R.id.effects_box) as CheckBox).isChecked
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.game_hud)
        val bubbleGLSurfaceView = BubbleGLSurfaceView(this)
        bubbleGLSurfaceView.isMuted(areSoundEffectsMuted)
        val glSurfaceViewHolder = findViewById<View>(R.id.GLSurfaceViewHolder) as FrameLayout
        glSurfaceViewHolder.addView(bubbleGLSurfaceView)
    }

    fun showGameOverScreenWith(score: String) {
        val glSurfaceViewHolder = this.findViewById<View>(R.id.GLSurfaceViewHolder) as FrameLayout?
        glSurfaceViewHolder?.removeAllViews()
        gameOverScreenLayout.showWith(score)
    }

    fun showHighscores(view: View) {
        highscoreLayout.show()
    }

    fun backToMenu(view: View) {
        mainMenuLayout.show()
    }

    fun setMusic(view: View) {
        val isMusicMuted = !(findViewById<View>(R.id.music_box) as CheckBox).isChecked
        musicPlayer.isMuted = isMusicMuted
    }

    fun setEffects(view: View) {
        areSoundEffectsMuted = !(findViewById<View>(R.id.effects_box) as CheckBox).isChecked
    }

    fun saveUsername(view: View) {
        val value = (findViewById<View>(R.id.username_field) as EditText).text.toString()
        if (value.isBlank()) {
            Toast.makeText(this, "sorry, username can not be empty!", LENGTH_SHORT).show()
            return
        }
        if (value.length > 10) {
            Toast.makeText(this, "sorry, maximal 10 letters!", LENGTH_SHORT).show()
            return
        }

        try {
            val isSuccess = DataConnection.registerUsername(value)[4000, TimeUnit.MILLISECONDS]
            if (!isSuccess) {
                Toast.makeText(this, "username already exists!", LENGTH_SHORT).show()
                return
            }
            username = value
            localFileStorage.writeToFile(username)
            mainMenuLayout.show()
        } catch (timeoutException: TimeoutException) {
            Toast.makeText(this, "Server is currently not available...please try again later.", LENGTH_LONG).show()
        }
    }

    fun showUsernameScreen(view: View) {
        usernameLayout.showWith(true)
    }

    fun launchMarket(view: View) {
        val uri = Uri.parse("market://details?id=$packageName")
        val myAppLinkToMarket = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(myAppLinkToMarket)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, " unable to find market app", LENGTH_LONG).show()
        }
    }

}