package org.codeberg.scovillo.bubble.log

import android.util.Log
import org.codeberg.scovillo.bubble.BuildConfig
import java.util.UUID

enum class LogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR }

/**
 * Central logging facade: respects a minimum log level (everything in debug
 * builds, warnings and up in release) and lets callers tag messages with a
 * request id so a single network call's full processing can be filtered for
 * in Logcat.
 */
object AppLogger {

    var minLevel: LogLevel = if (BuildConfig.DEBUG) LogLevel.VERBOSE else LogLevel.WARN

    fun newRequestId(): String = UUID.randomUUID().toString()

    fun v(tag: String, message: String, requestId: String? = null) =
        log(LogLevel.VERBOSE, tag, message, null, requestId)

    fun d(tag: String, message: String, requestId: String? = null) =
        log(LogLevel.DEBUG, tag, message, null, requestId)

    fun i(tag: String, message: String, requestId: String? = null) =
        log(LogLevel.INFO, tag, message, null, requestId)

    fun w(tag: String, message: String, throwable: Throwable? = null, requestId: String? = null) =
        log(LogLevel.WARN, tag, message, throwable, requestId)

    fun e(tag: String, message: String, throwable: Throwable? = null, requestId: String? = null) =
        log(LogLevel.ERROR, tag, message, throwable, requestId)

    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?, requestId: String?) {
        if (level < minLevel) return
        val prefixedMessage = if (requestId != null) "[reqId=$requestId] $message" else message
        when (level) {
            LogLevel.VERBOSE -> if (throwable != null) Log.v(tag, prefixedMessage, throwable) else Log.v(tag, prefixedMessage)
            LogLevel.DEBUG -> if (throwable != null) Log.d(tag, prefixedMessage, throwable) else Log.d(tag, prefixedMessage)
            LogLevel.INFO -> if (throwable != null) Log.i(tag, prefixedMessage, throwable) else Log.i(tag, prefixedMessage)
            LogLevel.WARN -> if (throwable != null) Log.w(tag, prefixedMessage, throwable) else Log.w(tag, prefixedMessage)
            LogLevel.ERROR -> if (throwable != null) Log.e(tag, prefixedMessage, throwable) else Log.e(tag, prefixedMessage)
        }
    }
}
