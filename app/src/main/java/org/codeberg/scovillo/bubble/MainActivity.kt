package org.codeberg.scovillo.bubble

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT
import org.codeberg.scovillo.bubble.data.ApiService
import org.codeberg.scovillo.bubble.data.LocalFileStorage
import org.codeberg.scovillo.bubble.data.SettingsModel
import org.codeberg.scovillo.bubble.screen.BubbleGLSurfaceView
import org.codeberg.scovillo.bubble.screen.layout.GameOverScreenLayout
import org.codeberg.scovillo.bubble.screen.layout.HighscoreLayout
import org.codeberg.scovillo.bubble.screen.layout.MainMenuLayout
import org.codeberg.scovillo.bubble.screen.layout.UsernameCreationLayout
import org.codeberg.scovillo.bubble.screen.layout.UsernameSelectionLayout
import org.codeberg.scovillo.bubble.sound.MusicPlayer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val THREAD_POOL: ExecutorService = Executors.newCachedThreadPool()

class MainActivity : Activity() {

    val settingsModel = SettingsModel()

    private val musicPlayer = MusicPlayer(this)
    private val localFileStorage = LocalFileStorage(this)

    private val gameOverScreenLayout = GameOverScreenLayout(this)
    private val usernameSelectionLayout = UsernameSelectionLayout(this)
    private val usernameCreationLayout = UsernameCreationLayout(this)
    private val highscoreLayout = HighscoreLayout(this)
    private val mainMenuLayout = MainMenuLayout(this, musicPlayer)

    lateinit var selectedUser: UserResource
        private set
    private var users = mutableListOf<UserResource>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsModel.load(this)
        musicPlayer.init()
        users = localFileStorage.readFromFile()
        if(users.isEmpty()) {
            usernameCreationLayout.showWith(false)
        } else {
            usernameSelectionLayout.showWith(users)
        }
    }

    public override fun onResume() {
        super.onResume()
        musicPlayer.isMuted = settingsModel.isMusicMuted
    }

    public override fun onPause() {
        super.onPause()
        musicPlayer.pause()
    }

    fun startGame(view: View) {
        mainMenuLayout.hide()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.game_hud)
        val bubbleGLSurfaceView = BubbleGLSurfaceView(this)
        bubbleGLSurfaceView.isMuted(settingsModel.areSoundEffectsMuted)
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
        if (view is CheckBox) {
            settingsModel.isMusicMuted = !view.isChecked
            musicPlayer.isMuted = settingsModel.isMusicMuted
            settingsModel.save(this)
        }
    }

    fun setEffects(view: View) {
        if (view is CheckBox) {
            settingsModel.areSoundEffectsMuted = !view.isChecked
            settingsModel.save(this)
        }
    }

    fun saveUsername(view: View) {
        val value = (findViewById<View>(R.id.username_field) as EditText).text.toString()
        if (value.isBlank()) {
            Toast.makeText(this, getString(R.string.error_empty_username), LENGTH_SHORT).show()
            return
        }
        if (value.length > 10) {
            Toast.makeText(this, getString(R.string.error_username_too_long), LENGTH_SHORT).show()
            return
        }
        try {
            val created = ApiService.registerUsername(value)[6000, TimeUnit.MILLISECONDS]
            users.add(created)
            localFileStorage.writeToFile(users)
            this.selectUser(created)
        } catch (exception: Exception) {
            Toast.makeText(this, getString(R.string.error_username_exists), LENGTH_SHORT).show()
            Toast.makeText(this, getString(R.string.server_unavailable), LENGTH_LONG).show()
        }
    }

    fun showUsernameSelectionScreen(view: View) {
        usernameSelectionLayout.showWith(users)
    }

    fun showUsernameCreationScreen(view: View) {
        usernameCreationLayout.showWith(users.isNotEmpty())
    }

    fun selectUser(user: UserResource) {
        this.selectedUser = user
        mainMenuLayout.show()
    }

    fun launchMarket(view: View) {
        val uri = Uri.parse("market://details?id=$packageName")
        val myAppLinkToMarket = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(myAppLinkToMarket)
        } catch (exception: Exception) {
            Toast.makeText(this, getString(R.string.error_market_app_not_found), LENGTH_LONG).show()
        }
    }

    fun openProjectWebsite(view: View) {
        val uri = Uri.parse("https://codeberg.org/scovillo/bubble-penetration")
        val myAppLinkToMarket = Intent(Intent.ACTION_VIEW, uri)
        startActivity(myAppLinkToMarket)
    }

}
