package de.spicysources.bubblepenetration.data

import android.content.Context
import android.util.Log
import de.spicysources.bubblepenetration.MainActivity
import java.io.*

class LocalFileStorage(private val mainActivity: MainActivity) {

    private val filename = "bubblePenetration"

    fun writeToFile(username: String) {
        try {
            val output = OutputStreamWriter(mainActivity.openFileOutput(filename, Context.MODE_PRIVATE))
            output.write(username)
            output.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: $e")
        }
    }

    fun readFromFile(): String {
        try {
            val inputStream: InputStream? = mainActivity.openFileInput(filename)
            if (inputStream != null) {
                val inputStreamReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                var receiveString: String? = ""
                val stringBuilder = StringBuilder()
                while (bufferedReader.readLine().also { receiveString = it } != null) {
                    stringBuilder.append(receiveString)
                }
                inputStream.close()
                return stringBuilder.toString()
            }
        } catch (e: FileNotFoundException) {
            Log.e("login activity", "File not found: $e")
        } catch (e: IOException) {
            Log.e("login activity", "Can not read file: $e")
        }
        return ""
    }

}