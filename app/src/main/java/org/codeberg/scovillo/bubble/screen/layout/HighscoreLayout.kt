package org.codeberg.scovillo.bubble.screen.layout

import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.*
import android.widget.Toast.LENGTH_LONG
import org.codeberg.scovillo.bubble.MainActivity
import org.codeberg.scovillo.bubble.R
import org.codeberg.scovillo.bubble.data.ApiService
import org.codeberg.scovillo.bubble.THREAD_POOL
import java.util.concurrent.TimeUnit

class HighscoreLayout(private val mainActivity: MainActivity) {

    fun show() {
        mainActivity.setContentView(R.layout.highscores)

        loadHighscoresAsync()

        (mainActivity.findViewById<View>(R.id.highscore_back_button) as Button).typeface = Typeface.createFromAsset(
            mainActivity.assets,
            "fonts/PLUMP.ttf"
        )
        (mainActivity.findViewById<View>(R.id.table_rank) as TextView).typeface = Typeface.createFromAsset(
            mainActivity.assets,
            "fonts/PLUMP.ttf"
        )
        (mainActivity.findViewById<View>(R.id.table_name) as TextView).typeface = Typeface.createFromAsset(
            mainActivity.assets,
            "fonts/PLUMP.ttf"
        )
        (mainActivity.findViewById<View>(R.id.table_score) as TextView).typeface = Typeface.createFromAsset(
            mainActivity.assets,
            "fonts/PLUMP.ttf"
        )
    }

    private fun generateHighscoreTextView(): TextView {
        val tv = TextView(mainActivity)
        tv.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT,
            0.25f
        )
        tv.gravity = 1
        tv.setTextColor(Color.WHITE)
        tv.textSize = 25f
        return tv
    }

    private fun loadHighscoresAsync() {
        THREAD_POOL.execute {
            val table = mainActivity.findViewById<View>(R.id.highscore_table) as TableLayout
            try {
                val highscoreRequest = ApiService.getHighscoreData()
                val jsonArray = highscoreRequest[8000, TimeUnit.MILLISECONDS]

                for (i in 0 until jsonArray.length()) {
                    val rank = generateHighscoreTextView()
                    rank.text = "${i + 1}"
                    rank.typeface = Typeface.createFromAsset(mainActivity.assets, "fonts/PLUMP.ttf")
                    val name = generateHighscoreTextView()
                    name.text = jsonArray.getJSONObject(i).getString("name")
                    name.typeface = Typeface.createFromAsset(mainActivity.assets, "fonts/PLUMP.ttf")
                    val score = generateHighscoreTextView()
                    score.text = jsonArray.getJSONObject(i).getString("score")
                    score.typeface = Typeface.createFromAsset(mainActivity.assets, "fonts/PLUMP.ttf")
                    val row = TableRow(mainActivity)
                    if (name.text == mainActivity.selectedUser.name) {
                        rank.setTextColor(Color.YELLOW)
                        name.setTextColor(Color.YELLOW)
                        score.setTextColor(Color.YELLOW)
                    }
                    mainActivity.runOnUiThread {
                        row.addView(rank)
                        row.addView(name)
                        row.addView(score)
                        table.addView(row)
                    }
                }
            } catch (exception: Exception) {
                mainActivity.runOnUiThread {
                    Toast.makeText(mainActivity, mainActivity.getString(R.string.server_unavailable), LENGTH_LONG).show()
                }
            }
        }
    }

}