package org.codeberg.scovillo.bubble.persistence

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.FileNotFoundException
import java.io.OutputStreamWriter

data class LocalHighscore(val username: String, val score: Int)

class LocalHighscoreStorage(private val context: Context) {
    private val filename = "offlineHighscores"

    @Synchronized
    fun read(): List<LocalHighscore> = readEntries().sortedByDescending { it.score }

    @Synchronized
    fun saveIfHigher(username: String, score: Int): Boolean {
        val entries = readEntries().associateBy { it.username }.toMutableMap()
        val previousEntry = entries[username]
        if (previousEntry != null && score <= previousEntry.score) return false

        entries[username] = LocalHighscore(username, score)
        writeEntries(entries.values.sortedByDescending { it.score })
        return true
    }

    private fun readEntries(): List<LocalHighscore> {
        try {
            context.openFileInput(filename).bufferedReader().use { reader ->
                return reader.lineSequence().mapNotNull { line ->
                    try {
                        val json = JSONObject(line)
                        LocalHighscore(json.getString("username"), json.getInt("score"))
                    } catch (exception: Exception) {
                        Log.w("LocalHighscoreStorage", "Ignoring invalid local highscore", exception)
                        null
                    }
                }.toList()
            }
        } catch (_: FileNotFoundException) {
            return emptyList()
        } catch (exception: Exception) {
            Log.e("LocalHighscoreStorage", "Could not read local highscores", exception)
            return emptyList()
        }
    }

    private fun writeEntries(entries: Collection<LocalHighscore>) {
        try {
            BufferedWriter(OutputStreamWriter(context.openFileOutput(filename, Context.MODE_PRIVATE))).use { writer ->
                entries.forEach { entry ->
                    writer.write(JSONObject().put("username", entry.username).put("score", entry.score).toString())
                    writer.newLine()
                }
            }
        } catch (exception: Exception) {
            Log.e("LocalHighscoreStorage", "Could not save local highscores", exception)
        }
    }
}
