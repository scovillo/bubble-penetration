package org.codeberg.scovillo.bubble.ui.layout

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import org.codeberg.scovillo.bubble.MainActivity
import org.codeberg.scovillo.bubble.R
import org.codeberg.scovillo.bubble.THREAD_POOL
import org.codeberg.scovillo.bubble.api.ApiService
import org.codeberg.scovillo.bubble.persistence.SettingsModel
import org.codeberg.scovillo.bubble.sound.MusicPlayer
import java.util.concurrent.TimeUnit

class SettingsLayout(
    private val mainActivity: MainActivity,
    private val settingsModel: SettingsModel,
    private val mainMenuLayout: MainMenuLayout,
    private val musicPlayer: MusicPlayer,
) {
    private var isShowing = false

    fun show(backendUrl: String? = null) {
        isShowing = true
        mainMenuLayout.hide()
        mainActivity.setContentView(R.layout.settings)

        val backendUrlField = mainActivity.findViewById<EditText>(R.id.backend_url_field)
        backendUrlField.setText(backendUrl ?: settingsModel.backendBaseUrl)
        mainActivity.findViewById<CheckBox>(R.id.music_box).isChecked = !settingsModel.isMusicMuted
        mainActivity.findViewById<CheckBox>(R.id.effects_box).isChecked = !settingsModel.areSoundEffectsMuted
        val onlineLeaderboardBox = mainActivity.findViewById<CheckBox>(R.id.online_leaderboard_box)
        onlineLeaderboardBox.isChecked = settingsModel.useOnlineLeaderboard
        setBackendSettingsEnabled(settingsModel.useOnlineLeaderboard)
        applyBubbleFont()
        backendUrlField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                backendUrlField.setBackgroundResource(R.drawable.input_background)
            }
        })

        mainActivity.findViewById<CheckBox>(R.id.music_box).setOnClickListener { setMusic(it) }
        mainActivity.findViewById<CheckBox>(R.id.effects_box).setOnClickListener { setEffects(it) }
        onlineLeaderboardBox.setOnClickListener { setOnlineLeaderboard(it) }
        mainActivity.findViewById<Button>(R.id.test_connection_button).setOnClickListener { testBackendConnection() }
        mainActivity.findViewById<Button>(R.id.reset_backend_url_button).setOnClickListener { resetBackendUrl() }
        mainActivity.findViewById<Button>(R.id.back_to_menu_button).setOnClickListener { backToMenu() }
    }

    fun restore(savedInstanceState: Bundle?): Boolean {
        if (savedInstanceState?.getBoolean(STATE_SHOWING, false) != true) return false
        show(savedInstanceState.getString(STATE_BACKEND_URL))
        return true
    }

    fun saveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_SHOWING, isShowing)
        mainActivity.findViewById<EditText?>(R.id.backend_url_field)?.let {
            outState.putString(STATE_BACKEND_URL, it.text.toString())
        }
    }

    fun isShowing(): Boolean = isShowing

    fun backToMenu() {
        isShowing = false
        mainMenuLayout.show()
    }

    private fun setMusic(view: View) {
        if (view is CheckBox) {
            settingsModel.isMusicMuted = !view.isChecked
            musicPlayer.isMuted = settingsModel.isMusicMuted
            settingsModel.save(mainActivity)
        }
    }

    private fun setEffects(view: View) {
        if (view is CheckBox) {
            settingsModel.areSoundEffectsMuted = !view.isChecked
            settingsModel.save(mainActivity)
        }
    }

    private fun setOnlineLeaderboard(view: View) {
        if (view !is CheckBox) return
        settingsModel.useOnlineLeaderboard = view.isChecked
        settingsModel.save(mainActivity)
        setBackendSettingsEnabled(view.isChecked)
    }

    private fun setBackendSettingsEnabled(enabled: Boolean) {
        mainActivity.findViewById<View>(R.id.backend_settings).apply {
            alpha = if (enabled) 1f else 0.4f
            isEnabled = enabled
        }
        listOf(R.id.backend_url_field, R.id.test_connection_button, R.id.reset_backend_url_button).forEach { id ->
            mainActivity.findViewById<View>(id).isEnabled = enabled
        }
    }

    private fun testBackendConnection() {
        val testedBaseUrl = backendUrlToTest() ?: return

        THREAD_POOL.execute {
            try {
                ApiService.testConnection(testedBaseUrl)[8, TimeUnit.SECONDS]
                mainActivity.runOnUiThread {
                    settingsModel.backendBaseUrl = testedBaseUrl
                    settingsModel.save(mainActivity)
                    ApiService.setBaseUrl(testedBaseUrl)
                    setConnectionFieldBackground(testedBaseUrl, R.drawable.input_background_success)
                    Toast.makeText(mainActivity, mainActivity.getString(R.string.connection_successful), Toast.LENGTH_SHORT).show()
                }
            } catch (exception: Exception) {
                mainActivity.runOnUiThread {
                    settingsModel.backendBaseUrl = SettingsModel.DEFAULT_BACKEND_BASE_URL
                    settingsModel.save(mainActivity)
                    ApiService.setBaseUrl(SettingsModel.DEFAULT_BACKEND_BASE_URL)
                    setConnectionFieldBackground(testedBaseUrl, R.drawable.input_background_failure)
                    Toast.makeText(mainActivity, mainActivity.getString(R.string.connection_failed), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun backendUrlToTest(): String? {
        val value = mainActivity.findViewById<EditText>(R.id.backend_url_field)?.text?.toString()?.trim()
            ?: return null
        if (value.isBlank() || !(value.startsWith("https://") || value.startsWith("http://"))) {
            mainActivity.findViewById<EditText>(R.id.backend_url_field)
                ?.setBackgroundResource(R.drawable.input_background_failure)
            Toast.makeText(mainActivity, mainActivity.getString(R.string.invalid_backend_url), Toast.LENGTH_LONG).show()
            return null
        }
        return value.trimEnd('/')
    }

    private fun resetBackendUrl() {
        settingsModel.backendBaseUrl = SettingsModel.DEFAULT_BACKEND_BASE_URL
        settingsModel.save(mainActivity)
        ApiService.setBaseUrl(settingsModel.backendBaseUrl)
        mainActivity.findViewById<EditText>(R.id.backend_url_field).setText(settingsModel.backendBaseUrl)
    }

    private fun applyBubbleFont() {
        val bubbleFont = Typeface.createFromAsset(mainActivity.assets, "fonts/PLUMP.ttf")
        listOf(
            R.id.settings_title,
            R.id.music_box,
            R.id.effects_box,
            R.id.online_leaderboard_box,
            R.id.backend_url_label,
            R.id.test_connection_button,
            R.id.reset_backend_url_button,
            R.id.back_to_menu_button,
        ).forEach { id ->
            mainActivity.findViewById<TextView>(id).typeface = bubbleFont
        }
    }

    private fun setConnectionFieldBackground(testedBaseUrl: String, backgroundRes: Int) {
        val backendUrlField = mainActivity.findViewById<EditText>(R.id.backend_url_field) ?: return
        if (backendUrlField.text.toString().trim().trimEnd('/') == testedBaseUrl) {
            backendUrlField.setBackgroundResource(backgroundRes)
        }
    }

    private companion object {
        const val STATE_SHOWING = "isShowingSettings"
        const val STATE_BACKEND_URL = "settingsUrl"
    }
}
