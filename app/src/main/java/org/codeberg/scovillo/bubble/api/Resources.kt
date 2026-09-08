package org.codeberg.scovillo.bubble.api

import org.json.JSONObject

// credential is the player's bearer secret; null for profiles created offline or before
// the game-session API existed, in which case online play falls back to local-only mode.
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
