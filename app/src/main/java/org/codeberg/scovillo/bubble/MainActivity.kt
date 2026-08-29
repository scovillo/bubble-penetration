package org.codeberg.scovillo.bubble

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import org.codeberg.scovillo.bubble.api.ApiService
import org.codeberg.scovillo.bubble.api.UserResource
import org.codeberg.scovillo.bubble.api.findHttpStatusException
import org.codeberg.scovillo.bubble.persistence.LocalFileStorage
import org.codeberg.scovillo.bubble.persistence.LocalHighscoreStorage
import org.codeberg.scovillo.bubble.persistence.SettingsModel
import org.codeberg.scovillo.bubble.sound.MusicPlayer
import org.codeberg.scovillo.bubble.sound.SoundEffects
import org.codeberg.scovillo.bubble.ui.BubbleFont
import org.codeberg.scovillo.bubble.ui.layout.GameOverScreenLayout
import org.codeberg.scovillo.bubble.ui.layout.HighscoreLayout
import org.codeberg.scovillo.bubble.ui.layout.MainMenuLayout
import org.codeberg.scovillo.bubble.ui.layout.SettingsLayout
import org.codeberg.scovillo.bubble.ui.layout.UsernameCreationLayout
import org.codeberg.scovillo.bubble.ui.layout.UsernameSelectionLayout
import org.codeberg.scovillo.bubble.ui.render.BubbleGLSurfaceView
import org.codeberg.scovillo.bubble.ui.render.GameBubbleScene
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val THREAD_POOL: ExecutorService = Executors.newCachedThreadPool()

private sealed interface GameSession {
    data object Inactive : GameSession

    data class Active(
        val view: BubbleGLSurfaceView,
        val scene: GameBubbleScene,
    ) : GameSession
}

class MainActivity : ComponentActivity() {

    val settingsModel = SettingsModel()

    private val musicPlayer = MusicPlayer(this)
    internal lateinit var soundEffects: SoundEffects
    private val localFileStorage = LocalFileStorage(this)
    val localHighscoreStorage = LocalHighscoreStorage(this)

    private val gameOverScreenLayout = GameOverScreenLayout(this)
    private val usernameSelectionLayout = UsernameSelectionLayout(this)
    private val usernameCreationLayout = UsernameCreationLayout(this)
    private val highscoreLayout = HighscoreLayout(this)
    private val mainMenuLayout = MainMenuLayout(this, musicPlayer)
    private val settingsLayout = SettingsLayout(this, settingsModel, mainMenuLayout, musicPlayer)

    lateinit var selectedUser: UserResource
        private set
    private var users = mutableListOf<UserResource>()

    private var isGameRunning = false
    private var gameSession: GameSession = GameSession.Inactive
    private var isUsingOfflineFallback = false

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        BubbleFont.applyTo(
            findViewById(android.R.id.content),
            scaleNonButtonText = layoutResID != R.layout.highscores,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        soundEffects = SoundEffects(this)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (settingsLayout.isShowing()) {
                    settingsLayout.backToMenu()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
        settingsModel.load(this)
        ApiService.setBaseUrl(settingsModel.backendBaseUrl)
        musicPlayer.init()
        users = localFileStorage.readFromFile()

        var restoredScore = 0
        var restoredTimer = 25.0f

        if (savedInstanceState != null) {
            isGameRunning = savedInstanceState.getBoolean("isGameRunning", false)
            val userName = savedInstanceState.getString("selectedUserName")
            if (userName != null) {
                selectedUser = UserResource(userName)
            }
            restoredScore = savedInstanceState.getInt("savedScore", 0)
            restoredTimer = savedInstanceState.getFloat("savedTimer", 25.0f)
        }

        when {
            isGameRunning -> {
                mainMenuLayout.show()
                startGame(null, restoredScore, restoredTimer)
            }
            settingsLayout.restore(savedInstanceState) -> Unit
            ::selectedUser.isInitialized -> {
                mainMenuLayout.show()
            }
            users.isEmpty() -> {
                usernameCreationLayout.showWith(false)
            }
            else -> {
                usernameSelectionLayout.showWith(users)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isGameRunning", isGameRunning)
        settingsLayout.saveInstanceState(outState)
        if (::selectedUser.isInitialized) {
            outState.putString("selectedUserName", selectedUser.username)
        }
        val currentSession = gameSession
        if (currentSession is GameSession.Active) {
            outState.putInt("savedScore", currentSession.scene.getScore())
            outState.putFloat("savedTimer", currentSession.scene.getTimer())
        }
    }

    public override fun onResume() {
        super.onResume()
        val currentSession = gameSession
        if (currentSession is GameSession.Active) {
            currentSession.view.resumeScene()
        }
        musicPlayer.isMuted = settingsModel.isMusicMuted
    }

    public override fun onPause() {
        val currentSession = gameSession
        if (currentSession is GameSession.Active) {
            currentSession.view.pauseScene()
        }
        super.onPause()
        musicPlayer.pause()
    }

    @JvmOverloads
    fun startGame(view: View?, score: Int = 0, timer: Float = 25.0f) {
        isGameRunning = true
        mainMenuLayout.hide()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.game_hud)
        val scene = GameBubbleScene(this, score, timer, settingsModel.areSoundEffectsMuted)
        val bubbleGLSurfaceView = BubbleGLSurfaceView(this, scene)
        gameSession = GameSession.Active(bubbleGLSurfaceView, scene)
        val glSurfaceViewHolder = findViewById<View>(R.id.GLSurfaceViewHolder) as FrameLayout
        glSurfaceViewHolder.addView(bubbleGLSurfaceView)
    }

    fun showGameOverScreenWith(score: String) {
        isGameRunning = false
        gameSession = GameSession.Inactive
        val glSurfaceViewHolder = this.findViewById<View>(R.id.GLSurfaceViewHolder) as FrameLayout?
        glSurfaceViewHolder?.removeAllViews()
        gameOverScreenLayout.showWith(score)
    }

    fun showHighscores(view: View) {
        highscoreLayout.show()
    }

    fun backToMenu(view: View) {
        isGameRunning = false
        gameSession = GameSession.Inactive
        mainMenuLayout.show()
    }

    fun showSettings(view: View) {
        settingsLayout.show()
    }

    override fun onDestroy() {
        if (::soundEffects.isInitialized) {
            soundEffects.release()
        }
        super.onDestroy()
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
        if (!settingsModel.useOnlineLeaderboard) {
            val created = UserResource(value)
            users.add(created)
            localFileStorage.writeToFile(users)
            selectUser(created)
            return
        }
        try {
            val created = ApiService.registerUsername(value)[6000, TimeUnit.MILLISECONDS]
            onBackendRequestSucceeded()
            users.add(created)
            localFileStorage.writeToFile(users)
            this.selectUser(created)
        } catch (exception: Exception) {
            val httpException = exception.findHttpStatusException()
            if (httpException?.statusCode == 409) {
                Toast.makeText(this, getString(R.string.error_username_exists), LENGTH_SHORT).show()
                return
            }
            if (showRateLimitMessage(exception)) return
            val created = UserResource(value)
            users.add(created)
            localFileStorage.writeToFile(users)
            selectUser(created)
            showOfflineFallbackMessageOnce()
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

    @Synchronized
    fun onBackendRequestSucceeded() {
        isUsingOfflineFallback = false
    }

    @Synchronized
    fun showOfflineFallbackMessageOnce() {
        if (isUsingOfflineFallback) return
        isUsingOfflineFallback = true
        Toast.makeText(this, getString(R.string.server_unavailable_offline), LENGTH_LONG).show()
    }

    fun showRateLimitMessage(exception: Throwable): Boolean {
        val httpException = exception.findHttpStatusException()
        if (httpException?.statusCode != 429) return false

        val seconds = httpException.retryAfterSeconds?.coerceAtLeast(1) ?: 60
        runOnUiThread {
            val message = resources.getQuantityString(
                R.plurals.rate_limit_retry,
                seconds,
                seconds,
            )
            Toast.makeText(this, message, LENGTH_LONG).show()
        }
        return true
    }

    fun launchMarket(view: View) {
        val uri = Uri.parse("market://details?id=$packageName")
        val myAppLinkToMarket = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(myAppLinkToMarket)
        } catch (exception: Exception) {
            Toast.makeText(this, getString(R.string.error_market_app_not_found), LENGTH_LONG).show()
            exception.printStackTrace()
        }
    }

    fun openWebsite(view: View) {
        val uri = Uri.parse("https://scovillo.codeberg.page/bubble-penetration/")
        val websiteIntent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(websiteIntent)
    }

}
