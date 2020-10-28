package de.spicysources.bubblepenetration.data

import android.content.Context
import android.util.Log
import de.spicysources.bubblepenetration.MainActivity
import java.io.*

class LocalFileStorage(private val mainActivity: MainActivity) {

    private val filename = "bubblePenetration"

    fun writeToFile(usernames: List<String>) {
        try {
            val output = OutputStreamWriter(mainActivity.openFileOutput(filename, Context.MODE_PRIVATE))
            val outputWriter = BufferedWriter(output)
            usernames.forEach {
                outputWriter.write(it)
                outputWriter.newLine()
            }
            outputWriter.flush()
            outputWriter.close()
            output.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: $e")
        }
    }

    fun readFromFile(): MutableList<String> {
        try {
            val inputStream: InputStream? = mainActivity.openFileInput(filename)
            if (inputStream != null) {
                val inputStreamReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                var readUsername: String?
                val usernames = mutableListOf<String>()
                while (bufferedReader.readLine().also { readUsername = it } != null) {
                    usernames.add(readUsername!!)
                }
                inputStream.close()
                return usernames
            }
        } catch (e: FileNotFoundException) {
            Log.e("login activity", "File not found: $e")
        } catch (e: IOException) {
            Log.e("login activity", "Can not read file: $e")
        }
        return mutableListOf()
    }

}