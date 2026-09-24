package org.codeberg.scovillo.bubble.android.api

import org.json.JSONObject

class UserResource(
    val username: String,
    val credential: String? = null,
    val isOfflineOnly: Boolean = false,
) {
    fun toJson(): JSONObject {
        val json = JSONObject("{}")
        json.put("username", username)
        if (credential != null) {
            json.put("credential", credential)
        }
        if (isOfflineOnly) {
            json.put("offlineOnly", true)
        }
        return json
    }
}

data class OnlineGameSession(
    val sessionId: String,
    val seed: String,
    val replayVersion: Int,
)
