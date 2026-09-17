package org.codeberg.scovillo.bubble.android.persistence

import android.content.Context

data class MatchSettings(
    val isMusicMuted: Boolean,
    val areSoundEffectsMuted: Boolean,
    val isVibrationEnabled: Boolean,
)

class SettingsModel {
    companion object {
        const val DEFAULT_BACKEND_BASE_URL = "https://dev.bubble.lukas-scheerer.de"
    }

    var isMusicMuted: Boolean = false
    var areSoundEffectsMuted: Boolean = false
    var isVibrationEnabled: Boolean = true
    var useOnlineLeaderboard: Boolean = true
    var backendBaseUrl: String = DEFAULT_BACKEND_BASE_URL

    fun save(context: Context) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("music_muted", isMusicMuted)
            putBoolean("effects_muted", areSoundEffectsMuted)
            putBoolean("vibration_enabled", isVibrationEnabled)
            putBoolean("use_online_leaderboard", useOnlineLeaderboard)
            putString("backend_base_url", backendBaseUrl)
            apply()
        }
    }

    fun load(context: Context) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        isMusicMuted = prefs.getBoolean("music_muted", false)
        areSoundEffectsMuted = prefs.getBoolean("effects_muted", false)
        isVibrationEnabled = prefs.getBoolean("vibration_enabled", true)
        useOnlineLeaderboard = prefs.getBoolean("use_online_leaderboard", true)
        backendBaseUrl = prefs.getString("backend_base_url", DEFAULT_BACKEND_BASE_URL)
            ?.trim()
            ?.ifBlank { DEFAULT_BACKEND_BASE_URL }
            ?: DEFAULT_BACKEND_BASE_URL
    }

    fun toMatchSettings(): MatchSettings {
        return MatchSettings(
            isMusicMuted = isMusicMuted,
            areSoundEffectsMuted = areSoundEffectsMuted,
            isVibrationEnabled = isVibrationEnabled,
        )
    }
}
