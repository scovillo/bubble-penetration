package org.codeberg.scovillo.bubble.android

import android.R
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.codeberg.scovillo.bubble.android.api.ApiService
import org.codeberg.scovillo.bubble.android.api.OnlineGameSession
import org.codeberg.scovillo.bubble.android.api.UserResource
import org.codeberg.scovillo.bubble.android.api.findHttpStatusException
import org.codeberg.scovillo.bubble.android.log.AppLogger
import org.codeberg.scovillo.bubble.android.persistence.LocalFileStorage
import org.codeberg.scovillo.bubble.android.persistence.LocalHighscoreStorage
import org.codeberg.scovillo.bubble.android.persistence.SettingsModel
import org.codeberg.scovillo.bubble.android.profile.ProfileManagement
import org.codeberg.scovillo.bubble.android.sound.MusicPlayer
import org.codeberg.scovillo.bubble.android.sound.SoundEffects
import org.codeberg.scovillo.bubble.android.ui.BubbleFont
import org.codeberg.scovillo.bubble.android.ui.hud.GamePreparationOverlay
import org.codeberg.scovillo.bubble.android.ui.hud.MatchFinishOverlay
import org.codeberg.scovillo.bubble.android.ui.layout.GameOverScreenLayout
import org.codeberg.scovillo.bubble.android.ui.layout.HighscoreLayout
import org.codeberg.scovillo.bubble.android.ui.layout.MainMenuLayout
import org.codeberg.scovillo.bubble.android.ui.layout.SettingsLayout
import org.codeberg.scovillo.bubble.android.ui.layout.UsernameCreationLayout
import org.codeberg.scovillo.bubble.android.ui.layout.UsernameSelectionLayout
import org.codeberg.scovillo.bubble.android.ui.render.BubbleGLSurfaceView
import org.codeberg.scovillo.bubble.android.ui.render.GameBubbleScene
import org.codeberg.scovillo.bubble.game.Boundaries
import org.codeberg.scovillo.bubble.game.GameActionEvent
import org.codeberg.scovillo.bubble.game.MatchState
import org.codeberg.scovillo.bubble.game.engine.DeterministicMatchEngine
import org.codeberg.scovillo.bubble.game.engine.MatchEngineConfig
import java.security.SecureRandom
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

    private companion object {
        const val SCREEN_TRANSITION_DURATION_MS = 800L
    }

    val settingsModel = SettingsModel()

    private val musicPlayer = MusicPlayer(this)
    internal lateinit var soundEffects: SoundEffects
    private val localFileStorage = LocalFileStorage(this)
    val localHighscoreStorage = LocalHighscoreStorage(this)
    private lateinit var profileManagement: ProfileManagement

    private val gameOverScreenLayout = GameOverScreenLayout(this)
    private val usernameSelectionLayout = UsernameSelectionLayout(this)
    private val usernameCreationLayout = UsernameCreationLayout(this)
    private val highscoreLayout = HighscoreLayout(this)
    private val mainMenuLayout = MainMenuLayout(this, musicPlayer)
    private val settingsLayout = SettingsLayout(this, settingsModel, mainMenuLayout, musicPlayer)

    val selectedUser: UserResource
        get() = profileManagement.selectedUser

    private var isGameRunning = false
    private var gameSession: GameSession = GameSession.Inactive
    private var isUsingOfflineFallback = false

    var lastGameActionLog: List<GameActionEvent> = emptyList()
        private set
    var lastGameDurationMs: Long = 0
        private set
    var lastGameViewportAspectRatio: Float = 1.0f
        private set
    private var gameLaunchGeneration = 0

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        val content = findViewById<View>(R.id.content)
        BubbleFont.applyTo(
            content,
            scaleNonButtonText = layoutResID != org.codeberg.scovillo.bubble.R.layout.highscores,
        )
        content.alpha = 0f
        content.animate()
            .alpha(1f)
            .setDuration(SCREEN_TRANSITION_DURATION_MS)
            .start()
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
        profileManagement = ProfileManagement(localFileStorage)
        var restoredScore = 0
        var restoredTimer = -1f
        var restartRunningGame = false

        if (savedInstanceState != null) {
            isGameRunning = savedInstanceState.getBoolean("isGameRunning", false)
            restartRunningGame = savedInstanceState.getBoolean("restartRunningGame", false)
            val userName = savedInstanceState.getString("selectedUserName")
            if (userName != null) {
                profileManagement.restoreSelectedProfile(
                    UserResource(
                        userName,
                        savedInstanceState.getString("selectedUserCredential"),
                        savedInstanceState.getBoolean("selectedUserOfflineOnly", false),
                    ),
                )
            }
            restoredScore = savedInstanceState.getInt("savedScore", 0)
            restoredTimer = savedInstanceState.getFloat("savedTimer")
        }

        when {
            isGameRunning -> {
                mainMenuLayout.show()
                if (restartRunningGame) {
                    startGame(null)
                } else {
                    startGame(null, restoredScore, restoredTimer)
                }
            }

            settingsLayout.restore(savedInstanceState) -> Unit
            profileManagement.hasSelectedProfile -> {
                mainMenuLayout.show()
                migrateSelectedLegacyProfileIfNeeded()
            }

            profileManagement.profiles.isEmpty() -> {
                usernameCreationLayout.showWith(false)
            }

            else -> {
                usernameSelectionLayout.showWith(profileManagement.profiles)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isGameRunning", isGameRunning)
        outState.putBoolean("restartRunningGame", isChangingConfigurations && isGameRunning)
        settingsLayout.saveInstanceState(outState)
        if (profileManagement.hasSelectedProfile) {
            outState.putString("selectedUserName", selectedUser.username)
            outState.putString("selectedUserCredential", selectedUser.credential)
            outState.putBoolean("selectedUserOfflineOnly", selectedUser.isOfflineOnly)
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
    fun startGame(view: View?, score: Int = 0, timer: Float = -1f) {
        isGameRunning = true
        val launchGeneration = ++gameLaunchGeneration
        lastGameActionLog = emptyList()
        lastGameDurationMs = 0
        lastGameViewportAspectRatio = 1.0f
        mainMenuLayout.hide()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(org.codeberg.scovillo.bubble.R.layout.game_hud)
        applyGameHudInsets()
        val credential = if (profileManagement.hasSelectedProfile) selectedUser.credential else null
        val isOnlineMatch = settingsModel.useOnlineLeaderboard && credential != null && score == 0
                && timer < 0
        showGamePreparation(launchGeneration) {
            if (isOnlineMatch) {
                startOnlineMatch(score, timer, credential, launchGeneration)
            } else {
                launchOfflineGameScene(score, timer)
            }
        }
    }

    private fun startOnlineMatch(
        score: Int,
        timer: Float,
        credential: String,
        launchGeneration: Int,
    ) {
        val sessionFuture = ApiService.createGameSession(credential)
        THREAD_POOL.execute {
            try {
                val session = sessionFuture[6000, TimeUnit.MILLISECONDS]
                runOnUiThread {
                    if (!isGameRunning || launchGeneration != gameLaunchGeneration) return@runOnUiThread
                    onBackendRequestSucceeded()
                    launchOnlineGameScene(score, timer, session)
                }
            } catch (exception: Exception) {
                runOnUiThread {
                    if (!isGameRunning || launchGeneration != gameLaunchGeneration) return@runOnUiThread
                    launchOfflineGameScene(score, timer)
                    if (!showRateLimitMessage(exception)) {
                        showOfflineFallbackMessageOnce()
                    }
                }
                exception.printStackTrace()
            }
        }
    }

    private fun showGamePreparation(
        launchGeneration: Int,
        onFinished: () -> Unit,
    ) {
        val holder =
            findViewById<FrameLayout>(org.codeberg.scovillo.bubble.R.id.GLSurfaceViewHolder)
        GamePreparationOverlay(this, holder).show(
            isActive = { isGameRunning && launchGeneration == gameLaunchGeneration },
            onFinished = onFinished,
        )
    }

    private fun launchOnlineGameScene(
        score: Int,
        timer: Float,
        onlineSession: OnlineGameSession,
    ) {
        val replaySeed = onlineSession.seed
        val config = MatchEngineConfig(
            replaySeed,
            onlineSession.replayVersion,
        )
        AppLogger.d("BubbleGame", "Engine config: ${config.summary()}")
        if (onlineSession.replayVersion != config.replayVersion) {
            throw IllegalStateException("Unsupported replay version ${onlineSession.replayVersion}")
        }
        val state = MatchState(
            score = score,
            timerValueMs = if (timer < 0) config.initialTimerSeconds else timer
        )
        val boundaries = Boundaries()
        val scene = GameBubbleScene(
            this,
            matchSettings = settingsModel.toMatchSettings(),
            engine = DeterministicMatchEngine(
                config = config,
                state = state,
                boundaries = boundaries
            ),
            session = onlineSession
        )
        showGameScene(scene)
    }

    private fun launchOfflineGameScene(score: Int, timer: Float) {
        val replaySeed = ByteArray(32).also(SecureRandom()::nextBytes)
            .joinToString("") { "%02x".format(it) }
        val config = MatchEngineConfig(replaySeed)
        AppLogger.d("BubbleGame", "Engine config: ${config.summary()}")
        val state = MatchState(
            score = score,
            timerValueMs = if (timer < 0) config.initialTimerSeconds else timer
        )
        val boundaries = Boundaries()
        val scene = GameBubbleScene(
            this,
            matchSettings = settingsModel.toMatchSettings(),
            engine = DeterministicMatchEngine(
                config = config,
                state = state,
                boundaries = boundaries
            ),
            session = null
        )
        showGameScene(scene)
    }

    private fun showGameScene(scene: GameBubbleScene) {
        val bubbleGLSurfaceView = BubbleGLSurfaceView(this, scene)
        gameSession = GameSession.Active(bubbleGLSurfaceView, scene)
        val glSurfaceViewHolder =
            findViewById<FrameLayout>(org.codeberg.scovillo.bubble.R.id.GLSurfaceViewHolder)
        glSurfaceViewHolder.addView(bubbleGLSurfaceView)
        scene.addFieldFrameOverlay()
    }

    fun showGameOverScreen(score: String, session: OnlineGameSession?) {
        isGameRunning = false
        val finishGeneration = ++gameLaunchGeneration
        val finishedSession = gameSession
        lastGameActionLog =
            (finishedSession as? GameSession.Active)?.scene?.getActionLog() ?: emptyList()
        lastGameDurationMs = (finishedSession as? GameSession.Active)?.scene?.getDurationMs() ?: 0
        lastGameViewportAspectRatio =
            (finishedSession as? GameSession.Active)?.scene?.getViewportAspectRatio() ?: 1.0f
        gameSession = GameSession.Inactive
        val glSurfaceViewHolder =
            this.findViewById<View>(org.codeberg.scovillo.bubble.R.id.GLSurfaceViewHolder) as FrameLayout?
        if (glSurfaceViewHolder == null) {
            gameOverScreenLayout.show(score, session)
            return
        }
        MatchFinishOverlay(this, glSurfaceViewHolder).show {
            if (finishGeneration == gameLaunchGeneration) {
                glSurfaceViewHolder.removeAllViews()
                gameOverScreenLayout.show(score, session)
            }
        }
    }

    fun showHighscores(view: View) {
        highscoreLayout.show()
    }

    fun backToMenu(view: View) {
        isGameRunning = false
        gameLaunchGeneration++
        gameSession = GameSession.Inactive
        mainMenuLayout.show()
    }

    fun showSettings(view: View) {
        settingsLayout.show()
    }

    override fun onDestroy() {
        gameLaunchGeneration++
        if (::soundEffects.isInitialized) {
            soundEffects.release()
        }
        super.onDestroy()
    }

    fun saveUsername(view: View) {
        val value = usernameFromInput()
        if (!isLocallyValidUsername(value)) return
        if (!settingsModel.useOnlineLeaderboard) {
            createLocalProfile(value)
            return
        }
        try {
            ApiService.validateUsername(value)[6000, TimeUnit.MILLISECONDS]
            val created = ApiService.registerUsername(value)[6000, TimeUnit.MILLISECONDS]
            onBackendRequestSucceeded()
            profileManagement.addProfile(created)
            this.selectUser(created)
        } catch (exception: Exception) {
            val httpException = exception.findHttpStatusException()
            if (httpException?.statusCode == 409) {
                Toast.makeText(
                    this,
                    getString(org.codeberg.scovillo.bubble.R.string.error_username_exists),
                    LENGTH_SHORT
                ).show()
                return
            }
            if (httpException?.statusCode == 400) {
                Toast.makeText(
                    this,
                    getString(org.codeberg.scovillo.bubble.R.string.error_username_not_allowed),
                    LENGTH_LONG
                ).show()
                return
            }
            if (showProfileRegistrationRateLimitMessage(exception)) return
            val created = UserResource(value)
            profileManagement.addProfile(created)
            selectUser(created)
            showOfflineFallbackMessageOnce()
        }
    }

    /** Creates a profile without consuming an online-registration attempt. */
    fun saveUsernameOffline(view: View) {
        val value = usernameFromInput()
        if (!isLocallyValidUsername(value)) return
        createLocalProfile(value)
    }

    private fun usernameFromInput(): String =
        (findViewById<View>(org.codeberg.scovillo.bubble.R.id.username_field) as EditText).text.toString()

    private fun isLocallyValidUsername(value: String): Boolean {
        val error = when {
            value.isBlank() -> org.codeberg.scovillo.bubble.R.string.error_empty_username
            value.length < 2 -> org.codeberg.scovillo.bubble.R.string.error_username_too_short
            value.length > 12 -> org.codeberg.scovillo.bubble.R.string.error_username_too_long
            else -> return true
        }
        Toast.makeText(this, getString(error), LENGTH_SHORT).show()
        return false
    }

    private fun createLocalProfile(username: String) {
        val created = profileManagement.createOfflineProfile(username)
        selectUser(created)
    }

    fun showUsernameSelectionScreen(view: View) {
        usernameSelectionLayout.showWith(profileManagement.profiles)
    }

    fun showUsernameCreationScreen(view: View) {
        usernameCreationLayout.showWith(profileManagement.profiles.isNotEmpty())
    }

    fun selectUser(user: UserResource) {
        profileManagement.selectProfile(user)
        mainMenuLayout.show()
        migrateSelectedLegacyProfileIfNeeded()
    }

    fun migrateSelectedLegacyProfileIfNeeded() {
        profileManagement.registerSelectedUserIfNeeded(
            settingsModel.useOnlineLeaderboard,
            postToUi = { action -> runOnUiThread { action() } },
            onRegistered = {
                onBackendRequestSucceeded()
            },
            onUsernameRejected = {
                Toast.makeText(
                    this,
                    getString(org.codeberg.scovillo.bubble.R.string.error_username_not_allowed),
                    LENGTH_LONG,
                ).show()
            },
            onFailure = { exception ->
                if (!showProfileRegistrationRateLimitMessage(exception)) {
                    showOfflineFallbackMessageOnce()
                }
            },
        )
    }

    @Synchronized
    fun onBackendRequestSucceeded() {
        isUsingOfflineFallback = false
    }

    @Synchronized
    fun showOfflineFallbackMessageOnce() {
        if (isUsingOfflineFallback) return
        isUsingOfflineFallback = true
        Toast.makeText(
            this,
            getString(org.codeberg.scovillo.bubble.R.string.server_unavailable_offline),
            LENGTH_LONG
        ).show()
    }

    fun showRateLimitMessage(exception: Throwable): Boolean {
        val httpException = exception.findHttpStatusException()
        if (httpException?.statusCode != 429) return false

        val seconds = httpException.retryAfterSeconds?.coerceAtLeast(1) ?: 60
        runOnUiThread {
            val message = if (seconds < 60) {
                resources.getQuantityString(
                    org.codeberg.scovillo.bubble.R.plurals.rate_limit_retry,
                    seconds,
                    seconds,
                )
            } else {
                getString(
                    org.codeberg.scovillo.bubble.R.string.rate_limit_retry_minutes,
                    seconds / 60,
                    seconds % 60,
                )
            }
            Toast.makeText(this, message, LENGTH_LONG).show()
        }
        return true
    }

    fun showProfileRegistrationRateLimitMessage(exception: Throwable): Boolean {
        val httpException = exception.findHttpStatusException()
        if (httpException?.statusCode != 429) return false

        val seconds = httpException.retryAfterSeconds?.coerceAtLeast(1) ?: 60
        val minutes = (seconds + 59) / 60
        runOnUiThread {
            Toast.makeText(
                this,
                getString(
                    org.codeberg.scovillo.bubble.R.string.rate_limit_profile_retry,
                    minutes,
                ),
                LENGTH_LONG,
            ).show()
        }
        return true
    }

    fun launchMarket(view: View) {
        val uri = Uri.parse("market://details?id=$packageName")
        val myAppLinkToMarket = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(myAppLinkToMarket)
        } catch (exception: Exception) {
            Toast.makeText(
                this,
                getString(org.codeberg.scovillo.bubble.R.string.error_market_app_not_found),
                LENGTH_LONG
            ).show()
            exception.printStackTrace()
        }
    }

    fun openWebsite(view: View) {
        val uri = Uri.parse("https://scovillo.codeberg.page/bubble-penetration/")
        val websiteIntent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(websiteIntent)
    }

    /**
     * Android 15+ always lays targetSdk 35+ apps edge-to-edge. Keep the game HUD
     * out of the status and navigation bars while the play field uses the rest
     * of the window.
     */
    private fun applyGameHudInsets() {
        val hud = findViewById<View>(org.codeberg.scovillo.bubble.R.id.hud)
        val initialLeft = hud.paddingLeft
        val initialTop = hud.paddingTop
        val initialRight = hud.paddingRight
        val initialBottom = hud.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(hud) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                initialLeft + systemBars.left,
                initialTop + systemBars.top,
                initialRight + systemBars.right,
                initialBottom + systemBars.bottom,
            )
            insets
        }
        ViewCompat.requestApplyInsets(hud)
    }

}
