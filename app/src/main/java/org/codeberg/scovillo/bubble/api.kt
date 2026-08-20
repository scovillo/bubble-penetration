package org.codeberg.scovillo.bubble

import org.json.JSONObject

class UserResource(val username: String) {
    fun toJson(): JSONObject {
        val json = JSONObject("{}")
        json.put("username", username)
        return json
    }
}

class HighscoreResource(val username: String, val score: Int)

class MatchStartResource(val id: String)

class MatchEndResource(val id: String, val score: Int, val isHighscore: Boolean)
