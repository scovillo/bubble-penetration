package de.spicysources.bubblepenetration.screen.layout

import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.*
import de.spicysources.bubblepenetration.MainActivity
import de.spicysources.bubblepenetration.R
import de.spicysources.bubblepenetration.data.DataConnection
import org.json.JSONArray
import java.util.concurrent.CompletableFuture

class HighscoreLayout(private val mainActivity: MainActivity) {

    fun show() {
        mainActivity.setContentView(R.layout.highscores)
        val table = mainActivity.findViewById<View>(R.id.highscore_table) as TableLayout
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

        val jsonArray: JSONArray? = CompletableFuture.supplyAsync {
            DataConnection.getHighscoreData()
        }.exceptionally { null }.join()

        if (jsonArray != null) {
            for (i in 0 until jsonArray.length()) {
                val rank = generateHighscoreTextView()
                rank.text = "${i + 1}"
                rank.typeface = Typeface.createFromAsset(mainActivity.assets, "fonts/PLUMP.ttf")
                val name = generateHighscoreTextView()
                name.text = jsonArray.getJSONObject(i).getString("username")
                name.typeface = Typeface.createFromAsset(mainActivity.assets, "fonts/PLUMP.ttf")
                val score = generateHighscoreTextView()
                score.text = jsonArray.getJSONObject(i).getString("highscore")
                score.typeface = Typeface.createFromAsset(mainActivity.assets, "fonts/PLUMP.ttf")
                val row = TableRow(mainActivity)
                if (name.text == mainActivity.username) {
                    rank.setTextColor(Color.YELLOW)
                    name.setTextColor(Color.YELLOW)
                    score.setTextColor(Color.YELLOW)
                }
                row.addView(rank)
                row.addView(name)
                row.addView(score)
                table.addView(row)
            }
        } else {
            Toast.makeText(mainActivity, "Server is currently not available...please try again later.", 1500).show()
        }
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
        tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
        return tv
    }

}