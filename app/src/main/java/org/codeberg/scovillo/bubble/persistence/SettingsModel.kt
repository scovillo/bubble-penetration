package org.codeberg.scovillo.bubble.persistence

import android.content.Context

class SettingsModel {
    var isMusicMuted: Boolean = false
    var areSoundEffectsMuted: Boolean = false

    fun save(context: Context) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("music_muted", isMusicMuted)
            putBoolean("effects_muted", areSoundEffectsMuted)
            apply()
        }
    }

    fun load(context: Context) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        isMusicMuted = prefs.getBoolean("music_muted", false)
        areSoundEffectsMuted = prefs.getBoolean("effects_muted", false)
    }
}
