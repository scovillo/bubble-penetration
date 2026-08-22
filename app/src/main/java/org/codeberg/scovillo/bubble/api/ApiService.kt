package org.codeberg.scovillo.bubble.api

import android.util.Log
import org.codeberg.scovillo.bubble.THREAD_POOL
import org.codeberg.scovillo.bubble.persistence.SettingsModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.Future

class HttpStatusException(val statusCode: Int, responseBody: String) : Exception(
    "HTTP $statusCode: $responseBody",
)

object ApiService {

    private const val TAG = "ApiService"
    @Volatile
    private var configuredBaseUrl = SettingsModel.DEFAULT_BACKEND_BASE_URL

    fun setBaseUrl(baseUrl: String) {
        configuredBaseUrl = baseUrl.trim().trimEnd('/')
    }

    private fun baseUrl(): String = configuredBaseUrl

    private fun apiUrl(path: String): String = "${baseUrl()}/api/v1/$path"

    fun testConnection(baseUrl: String): Future<Boolean> {
        return THREAD_POOL.submit(
            Callable {
                val url = "${baseUrl.trim().trimEnd('/')}/health"
                Log.d(TAG, "GET request to: $url")
                val httpConn = URL(url).openConnection() as HttpURLConnection
                httpConn.requestMethod = "GET"
                httpConn.connectTimeout = 6000
                httpConn.readTimeout = 6000
                try {
                    readResponseFrom(httpConn)
                    true
                } finally {
                    httpConn.disconnect()
                }
            }
        )
    }

    fun getHighscoreData(): Future<JSONArray> {
        return THREAD_POOL.submit(
            Callable {
                val url = apiUrl("highscores")
                Log.d(TAG, "GET request to: $url")
                val httpConn =
                    URL(url).openConnection() as HttpURLConnection
                httpConn.requestMethod = "GET"
                httpConn.doOutput = false
                try {
                    val result = readResponseFrom(httpConn)
                    Log.d(TAG, "Highscore response: $result")
                    val response = JSONObject(result)
                    return@Callable response.getJSONArray("highscores")
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
                val url = apiUrl("users")
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
                    if (!result.optBoolean("success", false)) {
                        throw IllegalStateException("Unexpected unsuccessful user registration response")
                    }
                    val user = result.getJSONObject("user")
                    val username = user.getString("username")
                    return@Callable UserResource(username)
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
                val url = apiUrl("highscores")
                Log.d(TAG, "POST request to: $url | payload: { username: $username, highscore: $score }")
                val httpConn = URL(url).openConnection() as HttpURLConnection
                httpConn.requestMethod = "POST"
                httpConn.doOutput = true

                val body = JSONObject("{}")
                body.put("username", username)
                body.put("highscore", score)

                sendPost(httpConn, body)

                try {
                    val result = readResponseFrom(httpConn)
                    Log.d(TAG, "Register highscore response: $result")
                    val response = JSONObject(result)
                    return@Callable response.getBoolean("isNewHighscore")
                } finally {
                    httpConn.disconnect()
                }
            }
        )
    }

    private fun readResponseFrom(httpURLConnection: HttpURLConnection): String {
        val responseCode = httpURLConnection.responseCode
        val responseStream = if (responseCode in 200..299) {
            httpURLConnection.inputStream
        } else {
            httpURLConnection.errorStream
        }

        if (responseStream == null) {
            throw IllegalStateException("HTTP $responseCode without response body")
        }

        val reader = BufferedReader(InputStreamReader(responseStream))
        var inputLine: String?
        val content = StringBuffer()
        while (reader.readLine().also { inputLine = it } != null) {
            content.append(inputLine)
        }
        reader.close()
        val responseBody = content.toString()

        if (responseCode !in 200..299) {
            throw HttpStatusException(responseCode, responseBody)
        }

        return responseBody
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
