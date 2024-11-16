package de.spicysources.bubblepenetration

import org.json.JSONObject

class UserResource(val id: String, val name: String) {
    fun toJson(): JSONObject {
        val json = JSONObject("{}")
        json.put("id", id)
        json.put("name", name)
        return json
    }
}

class HighscoreResource(val name: String, val score: Int)

class MatchStartResource(val id: String)

class MatchEndResource(val id: String, val score: Int, val isHighscore: Boolean)