package org.codeberg.scovillo.bubble.data

import android.util.Log
import org.codeberg.scovillo.bubble.BuildConfig
import org.codeberg.scovillo.bubble.THREAD_POOL
import org.codeberg.scovillo.bubble.UserResource
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.Future

object ApiService {

    private const val TAG = "ApiService"
    private val baseUrl = BuildConfig.BACKEND_BASEURL

    fun getHighscoreData(): Future<JSONArray> {
        return THREAD_POOL.submit(
            Callable {
                val url = "$baseUrl/v1/highscores"
                Log.d(TAG, "GET request to: $url")
                val httpConn =
                    URL(url).openConnection() as HttpURLConnection
                httpConn.requestMethod = "GET"
                httpConn.doOutput = false
                try {
                    val result = readResponseFrom(httpConn)
                    Log.d(TAG, "Highscore response: $result")
                    return@Callable JSONArray(result)
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching highscores", e)
                    throw e
                } finally {
                    httpConn.disconnect()
                }
            }
        )
    }

    fun registerUsername(username: String): Future<UserResource> {
        return THREAD_POOL.submit(
            Callable {
                val url = "$baseUrl/v1/username"
                Log.d(TAG, "POST request to: $url | payload: { username: $username }")
                val httpConn = URL(url).openConnection() as HttpURLConnection
                httpConn.requestMethod = "POST"
                httpConn.doOutput = true

                val body = JSONObject("{}")
                body.put("username", username)

                sendPost(httpConn, body)

                try {
                    val resultString = readResponseFrom(httpConn)
                    Log.d(TAG, "Register user response: $resultString")
                    val result = JSONObject(resultString)
                    return@Callable UserResource(result.getString("id"), result.getString("username"))
                } catch (e: Exception) {
                    Log.e(TAG, "Error registering username", e)
                    throw e
                } finally {
                    httpConn.disconnect()
                }
            }
        )
    }

    fun registerHighscore(username: String, score: String): Future<Boolean> {
        return THREAD_POOL.submit(
            Callable {
                val httpConn = URL(
                    "https://$baseUrl/v1/highscores"
                ).openConnection() as HttpURLConnection
                httpConn.requestMethod = "POST"
                httpConn.doOutput = true

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
