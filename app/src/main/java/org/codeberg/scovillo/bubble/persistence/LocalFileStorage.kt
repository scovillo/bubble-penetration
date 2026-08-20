package org.codeberg.scovillo.bubble.persistence

import android.content.Context
import android.util.Log
import org.codeberg.scovillo.bubble.MainActivity
import org.codeberg.scovillo.bubble.api.UserResource
import org.json.JSONObject
import java.io.*

class LocalFileStorage(private val mainActivity: MainActivity) {

    private val filename = "bubblePenetration"

    fun writeToFile(users: List<UserResource>) {
        try {
            val output = OutputStreamWriter(mainActivity.openFileOutput(filename, Context.MODE_PRIVATE))
            val outputWriter = BufferedWriter(output)
            users.forEach {
                outputWriter.write(it.toJson().toString())
                outputWriter.newLine()
            }
            outputWriter.flush()
            outputWriter.close()
            output.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: $e")
        }
    }

    fun readFromFile(): MutableList<UserResource> {
        try {
            val inputStream: InputStream? = mainActivity.openFileInput(filename)
            if (inputStream != null) {
                val inputStreamReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                var line: String?
                val users = mutableListOf<UserResource>()
                while (bufferedReader.readLine().also { line = it } != null) {
                    val json = JSONObject(line!!)
                    users.add(UserResource(json.getString("username")))
                }
                inputStream.close()
                return users
            }
        } catch (e: FileNotFoundException) {
            Log.e("LocalFileStorage", "File not found: $e")
        } catch (e: IOException) {
            Log.e("LocalFileStorage", "Can not read file: $e")
        }
        return mutableListOf()
    }

}