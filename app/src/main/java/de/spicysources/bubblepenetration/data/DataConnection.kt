package de.spicysources.bubblepenetration.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object DataConnection {

    fun getHighscoreData(): JSONArray {
        val httpConn = URL("https://bubble-dev.spicysources.de/highscores").openConnection() as HttpURLConnection
        httpConn.requestMethod = "GET"
        httpConn.doOutput = false
        val result = readResponseFrom(httpConn)
        return JSONObject(result).getJSONArray("highscores")
    }

    fun registerHighscore(username: String, score: String): Boolean {
        val httpConn = URL(
            "https://bubble-dev.spicysources.de/highscores"
        ).openConnection() as HttpURLConnection
        httpConn.requestMethod = "POST"
        httpConn.doOutput = true

        val body = JSONObject("{}")
        body.put("username", username)
        body.put("highscore", score)

        sendPost(httpConn, body)

        val result = readResponseFrom(httpConn)
        httpConn.disconnect()
        return result == "true"
    }

    fun registerUsername(username: String): Boolean {
        val httpConn = URL(
            "https://bubble-dev.spicysources.de/username"
        ).openConnection() as HttpURLConnection
        httpConn.requestMethod = "POST"
        httpConn.doOutput = true

        val body = JSONObject("{}")
        body.put("username", username)

        sendPost(httpConn, body)

        val result = readResponseFrom(httpConn)
        httpConn.disconnect()
        return result == "true"
    }

    private fun readResponseFrom(httpURLConnection: HttpURLConnection): String {
        val reader = BufferedReader(InputStreamReader(httpURLConnection.inputStream))
        var inputLine: String?
        val content = StringBuffer()
        while (reader.readLine().also { inputLine = it } != null) {
            content.append(inputLine)
        }
        reader.close()
        return content.toString()
    }

    private fun sendPost(httpURLConnection: HttpURLConnection, body: JSONObject) {
        httpURLConnection.setRequestProperty("Content-Type", "application/json")
        httpURLConnection.setRequestProperty("charset", "utf-8")
        val out = DataOutputStream(httpURLConnection.outputStream)
        out.write(body.toString().toByteArray())
        out.flush()
        out.close()
    }

}