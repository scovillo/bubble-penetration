package de.spicysources.bubblepenetration.data

import java.io.*
import java.net.HttpURLConnection
import java.net.URL

object DataConnection {

    fun getHighscoreData(username: String): Array<String> {
        var result = ""
        try {
            val httpConn = getHttpPostConnection("http://188.68.55.198:8080/BubbleHighscores/GetHighscores")
            val writer = BufferedWriter(OutputStreamWriter(httpConn!!.outputStream))
            writer.write(username.trimIndent())
            writer.flush()
            val reader =
                BufferedReader(InputStreamReader(httpConn.inputStream))
            result = reader.readLine()
            writer.close()
            reader.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return result.split("\\|").toTypedArray()
    }

    fun putHighscoreData(name: String?, score: String): Boolean {
        val inputString = "$name|$score\n"
        var better = false
        try {
            val httpConn = getHttpPostConnection("http://188.68.55.198:8080/BubbleHighscores/PutHighscores")
            val writer = BufferedWriter(OutputStreamWriter(httpConn!!.outputStream))
            writer.write(inputString)
            writer.flush()
            val reader = BufferedReader(InputStreamReader(httpConn.inputStream))
            val result = reader.read().toChar()
            if (result == '1') better = true
            writer.close()
            reader.close()
            httpConn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return better
    }

    fun getUsernameExists(username: String): Boolean {
        var exists = false
        try {
            val httpConn = getHttpPostConnection("http://188.68.55.198:8080/BubbleHighscores/CheckUsername")
            val writer = BufferedWriter(OutputStreamWriter(httpConn!!.outputStream))
            writer.write(username.trimIndent())
            writer.flush()
            val reader = BufferedReader(InputStreamReader(httpConn.inputStream))
            val result = reader.read().toChar()
            if (result == '1') exists = true
            writer.close()
            reader.close()
            httpConn.disconnect()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return exists
    }

    private fun getHttpPostConnection(url: String): HttpURLConnection? {
        var httpConn: HttpURLConnection? = null
        try {
            val conn = URL(url).openConnection()
            httpConn = conn as HttpURLConnection
            httpConn.requestMethod = "POST"
            httpConn.doOutput = true
            httpConn.connect()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return httpConn
    }
}