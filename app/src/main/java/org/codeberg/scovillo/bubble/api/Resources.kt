package org.codeberg.scovillo.bubble.api

import org.json.JSONObject

class UserResource(val username: String) {
    fun toJson(): JSONObject {
        val json = JSONObject("{}")
        json.put("username", username)
        return json
    }
}
