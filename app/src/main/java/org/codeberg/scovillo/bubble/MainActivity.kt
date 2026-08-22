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
import org.codeberg.scovillo.bubble.api.HttpStatusException
import org.codeberg.scovillo.bubble.persistence.LocalFileStorage
import org.codeberg.scovillo.bubble.persistence.LocalHighscoreStorage
import org.codeberg.scovillo.bubble.persistence.SettingsModel
import org.codeberg.scovillo.bubble.api.UserResource
import org.codeberg.scovillo.bubble.ui.BubbleGLSurfaceView
import org.codeberg.scovillo.bubble.ui.layout.GameOverScreenLayout
import org.codeberg.scovillo.bubble.ui.layout.HighscoreLayout
import org.codeberg.scovillo.bubble.ui.layout.MainMenuLayout
import org.codeberg.scovillo.bubble.ui.layout.SettingsLayout
import org.codeberg.scovillo.bubble.ui.layout.UsernameCreationLayout
import org.codeberg.scovillo.bubble.ui.layout.UsernameSelectionLayout
import org.codeberg.scovillo.bubble.sound.MusicPlayer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.ExecutionException

val THREAD_POOL: ExecutorService = Executors.newCachedThreadPool()

class MainActivity : ComponentActivity() {

    val settingsModel = SettingsModel()

    private val musicPlayer = MusicPlayer(this)
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
    private var currentBubbleView: BubbleGLSurfaceView? = null
    private var isUsingOfflineFallback = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        currentBubbleView?.let {
            outState.putInt("savedScore", it.getScore())
            outState.putFloat("savedTimer", it.getTimer())
        }
    }

    public override fun onResume() {
        super.onResume()
        currentBubbleView?.resumeGame()
        musicPlayer.isMuted = settingsModel.isMusicMuted
    }

    public override fun onPause() {
        currentBubbleView?.pauseGame()
        super.onPause()
        musicPlayer.pause()
    }

    @JvmOverloads
    fun startGame(view: View?, score: Int = 0, timer: Float = 25.0f) {
        isGameRunning = true
        mainMenuLayout.hide()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.game_hud)
        val bubbleGLSurfaceView = BubbleGLSurfaceView(this)
        bubbleGLSurfaceView.isMuted(settingsModel.areSoundEffectsMuted)

        if (score > 0 || timer != 25.0f) {
            bubbleGLSurfaceView.setGameState(score, timer)
        }

        currentBubbleView = bubbleGLSurfaceView
        val glSurfaceViewHolder = findViewById<View>(R.id.GLSurfaceViewHolder) as FrameLayout
        glSurfaceViewHolder.addView(bubbleGLSurfaceView)
    }

    fun showGameOverScreenWith(score: String) {
        isGameRunning = false
        currentBubbleView = null
        val glSurfaceViewHolder = this.findViewById<View>(R.id.GLSurfaceViewHolder) as FrameLayout?
        glSurfaceViewHolder?.removeAllViews()
        gameOverScreenLayout.showWith(score)
    }

    fun showHighscores(view: View) {
        highscoreLayout.show()
    }

    fun backToMenu(view: View) {
        isGameRunning = false
        currentBubbleView = null
        mainMenuLayout.show()
    }

    fun showSettings(view: View) {
        settingsLayout.show()
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
            val httpException = (exception as? ExecutionException)?.cause as? HttpStatusException
            if (httpException?.statusCode == 409) {
                Toast.makeText(this, getString(R.string.error_username_exists), LENGTH_SHORT).show()
                return
            }
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

    fun openProjectWebsite(view: View) {
        val uri = Uri.parse("https://codeberg.org/scovillo/bubble-penetration")
        val myAppLinkToMarket = Intent(Intent.ACTION_VIEW, uri)
        startActivity(myAppLinkToMarket)
    }

}
