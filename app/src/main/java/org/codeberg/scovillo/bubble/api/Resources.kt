package org.codeberg.scovillo.bubble.api

import org.json.JSONObject

class UserResource(val username: String, val credential: String? = null) {
    fun toJson(): JSONObject {
        val json = JSONObject("{}")
        json.put("username", username)
        if (credential != null) {
            json.put("credential", credential)
        }
        return json
    }
}

data class OnlineGameSession(
    val sessionId: String,
    val seed: String,
    val replayVersion: Int,
)
