package org.codeberg.scovillo.bubble.android.api

import org.codeberg.scovillo.bubble.android.THREAD_POOL
import org.codeberg.scovillo.bubble.android.log.AppLogger
import org.codeberg.scovillo.bubble.android.persistence.SettingsModel
import org.codeberg.scovillo.bubble.game.GameActionEvent
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Callable
import java.util.concurrent.Future

class HttpStatusException(
    val statusCode: Int,
    val retryAfterSeconds: Int?,
    responseBody: String,
) : Exception(
    "HTTP $statusCode: $responseBody",
)

fun Throwable.findHttpStatusException(): HttpStatusException? =
    generateSequence(this) { it.cause }
        .filterIsInstance<HttpStatusException>()
        .firstOrNull()

object ApiService {

    private const val TAG = "ApiService"

    @Volatile
    private var configuredBaseUrl = SettingsModel.DEFAULT_BACKEND_BASE_URL

    fun setBaseUrl(baseUrl: String) {
        configuredBaseUrl = baseUrl.trim().trimEnd('/')
    }

    private fun baseUrl(): String = configuredBaseUrl

    private fun apiUrl(path: String): String = "${baseUrl()}/api/v2/$path"

    fun testConnection(baseUrl: String): Future<Boolean> {
        return THREAD_POOL.submit(
            Callable {
                val requestId = AppLogger.newRequestId()
                val url = "${baseUrl.trim().trimEnd('/')}/health/live"
                AppLogger.d(TAG, "GET $url", requestId)
                val httpConn = URL(url).openConnection() as HttpURLConnection
                httpConn.requestMethod = "GET"
                httpConn.setRequestProperty("Origin", "app://org.codeberg.scovillo.bubble")
                httpConn.setRequestProperty("X-Request-Id", requestId)
                httpConn.connectTimeout = 6000
                httpConn.readTimeout = 6000
                try {
                    readResponseFrom(httpConn)
                    AppLogger.d(TAG, "GET $url succeeded", requestId)
                    true
                } catch (e: Exception) {
                    AppLogger.e(TAG, "GET $url failed", e, requestId)
                    throw e
                } finally {
                    httpConn.disconnect()
                }
            }
        )
    }

    fun getHighscorePage(username: String? = null, startRank: Int? = null): Future<JSONObject> {
        return THREAD_POOL.submit(
            Callable {
                val requestId = AppLogger.newRequestId()
                val query = when {
                    username != null -> "?username=${
                        URLEncoder.encode(
                            username,
                            Charsets.UTF_8.name()
                        )
                    }"

                    startRank != null -> "?startRank=$startRank"
                    else -> ""
                }
                val url = apiUrl("bubble-game/highscores$query")
                AppLogger.d(TAG, "GET $url", requestId)
                val httpConn = openConnection(url, "GET", requestId)
                try {
                    val result = JSONObject(readResponseFrom(httpConn))
                    AppLogger.d(TAG, "GET $url succeeded", requestId)
                    result
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Error fetching highscore page", e, requestId)
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
                val requestId = AppLogger.newRequestId()
                val url = apiUrl("players")
                AppLogger.d(TAG, "POST $url | payload: { username: $username }", requestId)
                val httpConn = openConnection(url, "POST", requestId)

                val body = JSONObject("{}")
                body.put("username", username)

                sendPost(httpConn, body)

                try {
                    val resultString = readResponseFrom(httpConn)
                    AppLogger.d(TAG, "Player registration succeeded", requestId)
                    val result = JSONObject(resultString)
                    return@Callable UserResource(
                        result.getString("username"),
                        result.getString("credential"),
                    )
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Error registering username", e, requestId)
                    throw e
                } finally {
                    httpConn.disconnect()
                }
            }
        )
    }

    fun createGameSession(credential: String): Future<OnlineGameSession> {
        return THREAD_POOL.submit(
            Callable {
                val requestId = AppLogger.newRequestId()
                val url = apiUrl("bubble-game/sessions")
                AppLogger.d(TAG, "POST $url", requestId)
                val httpConn = openConnection(url, "POST", requestId, credential)

                sendPost(httpConn, JSONObject("{}"))

                try {
                    val result = readResponseFrom(httpConn)
                    AppLogger.d(TAG, "Create game session response: $result", requestId)
                    val json = JSONObject(result)
                    OnlineGameSession(
                        sessionId = json.getString("sessionId"),
                        seed = json.getString("seed"),
                        replayVersion = json.getInt("replayVersion"),
                    )
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Error creating game session", e, requestId)
                    throw e
                } finally {
                    httpConn.disconnect()
                }
            }
        )
    }

    fun submitScore(
        credential: String,
        sessionId: String,
        score: Int,
        durationMs: Long,
        viewportAspectRatio: Float,
        events: List<GameActionEvent>,
    ): Future<Boolean> {
        return THREAD_POOL.submit(
            Callable {
                val requestId = AppLogger.newRequestId()
                val url = apiUrl("bubble-game/sessions/$sessionId")
                AppLogger.d(
                    TAG,
                    "PATCH $url | payload: { score: $score, events: ${events.size} }",
                    requestId
                )
                val httpConn = openConnection(url, "PATCH", requestId, credential)

                val body = JSONObject("{}")
                body.put("score", score)
                body.put("durationMs", durationMs)
                body.put("viewportAspectRatio", viewportAspectRatio)
                val eventsArray = JSONArray()
                events.forEach {
                    val eventJson = JSONObject("{}")
                    eventJson.put("objectId", it.objectId)
                    eventJson.put("timestampMs", it.timestampMs)
                    eventJson.put("x", it.x)
                    eventJson.put("y", it.y)
                    eventsArray.put(eventJson)
                }
                body.put("events", eventsArray)

                sendPost(httpConn, body)

                try {
                    val result = readResponseFrom(httpConn)
                    AppLogger.d(TAG, "Submit score response: $result", requestId)
                    JSONObject(result).getBoolean("isPersonalBest")
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Error submitting score", e, requestId)
                    throw e
                } finally {
                    httpConn.disconnect()
                }
            }
        )
    }

    private fun openConnection(
        url: String,
        method: String,
        requestId: String,
        credential: String? = null,
    ): HttpURLConnection {
        val httpConn = URL(url).openConnection() as HttpURLConnection
        httpConn.requestMethod = method
        httpConn.doOutput = method != "GET"
        httpConn.connectTimeout = REQUEST_TIMEOUT_MS
        httpConn.readTimeout = REQUEST_TIMEOUT_MS
        httpConn.setRequestProperty("Origin", "app://org.codeberg.scovillo.bubble")
        httpConn.setRequestProperty("X-Request-Id", requestId)
        if (credential != null) {
            httpConn.setRequestProperty("Authorization", "Bearer $credential")
        }
        return httpConn
    }

    private const val REQUEST_TIMEOUT_MS = 6_000

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
            val retryAfterSeconds = httpURLConnection
                .getHeaderField("Retry-After")
                ?.toIntOrNull()
            throw HttpStatusException(responseCode, retryAfterSeconds, responseBody)
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
