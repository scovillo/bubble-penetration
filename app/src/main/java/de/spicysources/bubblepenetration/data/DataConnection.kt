package de.spicysources.bubblepenetration.data

import android.util.Base64
import android.util.Log
import de.spicysources.bubblepenetration.BuildConfig
import de.spicysources.bubblepenetration.THREAD_POOL
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

    private val host = if (BuildConfig.DEBUG) "dev.bubble.spicysources.de" else "bubble.spicysources.de"

    private val basicUsername = "bubble-admin"
    private val basicPassword = "dioChWHuNM2aQzxyqIP8l6Ku5VEAnzbcypXL6vzZfOlhuUVLyu"

    private val apiVersion = "v1"

    fun getHighscoreData(): Future<JSONArray> {
        return THREAD_POOL.submit(
            Callable {
                val httpConn =
                    URL("https://$host/$apiVersion/highscores").openConnection() as HttpURLConnection
                httpConn.requestMethod = "GET"
                httpConn.doOutput = false
                httpConn.addBasicAuthorizationHeader()
                val result = readResponseFrom(httpConn)
                return@Callable JSONObject(result).getJSONArray("highscores")
            }
        )
    }

    fun registerHighscore(username: String, score: String): Future<Boolean> {
        return THREAD_POOL.submit(
            Callable {
                val httpConn = URL(
                    "https://$host/$apiVersion/highscores"
                ).openConnection() as HttpURLConnection
                httpConn.requestMethod = "POST"
                httpConn.doOutput = true
                httpConn.addBasicAuthorizationHeader()

                val body = JSONObject("{}")
                body.put("username", username)
                body.put("highscore", score)

                sendPost(httpConn, body)

                val result = readResponseFrom(httpConn)
                httpConn.disconnect()
                return@Callable result == "true"
            }
        )
    }

    fun registerUsername(username: String): Future<Boolean> {
        return THREAD_POOL.submit(
            Callable {
                val httpConn = URL(
                    "https://$host/$apiVersion/username"
                ).openConnection() as HttpURLConnection
                httpConn.requestMethod = "POST"
                httpConn.doOutput = true

                val body = JSONObject("{}")
                body.put("username", username)

                sendPost(httpConn, body)

                val result = readResponseFrom(httpConn)
                httpConn.disconnect()
                return@Callable result == "true"
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
        httpURLConnection.addBasicAuthorizationHeader()
        val out = DataOutputStream(httpURLConnection.outputStream)
        out.write(body.toString().toByteArray())
        out.flush()
        out.close()
    }

    private fun HttpURLConnection.addBasicAuthorizationHeader() {
        val headerValue = "Basic ${Base64.encodeToString("$basicUsername:$basicPassword".toByteArray(), Base64.NO_WRAP or Base64.URL_SAFE)}"
        Log.d("DEBUG", headerValue.trimIndent())
        this.setRequestProperty("Authorization", headerValue)
    }
}
