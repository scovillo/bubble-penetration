package de.spicysources.bubblepenetration.data

import de.spicysources.bubblepenetration.BuildConfig
import de.spicysources.bubblepenetration.MatchEndResource
import de.spicysources.bubblepenetration.MatchStartResource
import de.spicysources.bubblepenetration.THREAD_POOL
import de.spicysources.bubblepenetration.UserResource
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.Future

object DataConnection {

    private val host = if (BuildConfig.DEBUG) "dev.bubble.api.lukas-scheerer.de" else "bubble.api.lukas-scheerer.de"

    fun getHighscoreData(): Future<JSONArray> {
        return THREAD_POOL.submit(
            Callable {
                val httpConn =
                    URL("https://$host/matches/highscores").openConnection() as HttpURLConnection
                httpConn.requestMethod = "GET"
                httpConn.doOutput = false
                val result = readResponseFrom(httpConn)
                return@Callable JSONArray(result)
            }
        )
    }

    fun registerUsername(username: String): Future<UserResource> {
        return THREAD_POOL.submit(
            Callable {
                val httpConn = URL(
                    "https://$host/users"
                ).openConnection() as HttpURLConnection
                httpConn.requestMethod = "POST"
                httpConn.doOutput = true

                val body = JSONObject("{}")
                body.put("name", username)

                sendPost(httpConn, body)

                val result = JSONObject(readResponseFrom(httpConn))
                httpConn.disconnect()
                return@Callable UserResource(result.getString("id"), result.getString("name"))
            }
        )
    }

    fun startMatch(userId: String): Future<MatchStartResource> {
        return THREAD_POOL.submit(
            Callable {
                val httpConn = URL(
                    "https://$host/matches"
                ).openConnection() as HttpURLConnection
                httpConn.requestMethod = "POST"
                httpConn.doOutput = true

                val body = JSONObject("{}")
                body.put("userId", userId)

                sendPost(httpConn, body)

                val result = JSONObject(readResponseFrom(httpConn))
                httpConn.disconnect()
                return@Callable MatchStartResource(result.getString("id"))
            }
        )
    }

    fun endMatch(matchId: String, score: Int): Future<MatchEndResource> {
        return THREAD_POOL.submit(
            Callable {
                val httpConn = URL(
                    "https://$host/matches/$matchId"
                ).openConnection() as HttpURLConnection
                httpConn.requestMethod = "PUT"
                httpConn.doOutput = true

                val body = JSONObject("{}")
                body.put("score", score)

                sendPost(httpConn, body)

                val result = JSONObject(readResponseFrom(httpConn))
                httpConn.disconnect()
                return@Callable MatchEndResource(result.getString("id"), result.getInt("score"), result.getBoolean("isHighscore"))
            }
        )
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
