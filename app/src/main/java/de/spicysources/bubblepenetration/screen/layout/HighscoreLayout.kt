package de.spicysources.bubblepenetration.screen.layout

import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import de.spicysources.bubblepenetration.MainActivity
import de.spicysources.bubblepenetration.R
import de.spicysources.bubblepenetration.data.DataConnection

class HighscoreLayout(private val mainActivity: MainActivity) {

    fun show() {
        mainActivity.setContentView(R.layout.highscores)
        DataConnection.permitNetwork()
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
        val data = DataConnection.getHighscoreData(mainActivity.username);
        var i = 1
        while (i < data.size - 2) {
            val rank = generateHighscoreTextView()
            rank.text = data[i]
            rank.typeface = Typeface.createFromAsset(mainActivity.assets, "fonts/PLUMP.ttf")
            val name = generateHighscoreTextView()
            name.text = data[i + 1]
            name.typeface = Typeface.createFromAsset(mainActivity.assets, "fonts/PLUMP.ttf")
            val score = generateHighscoreTextView()
            score.text = data[i + 2]
            score.typeface = Typeface.createFromAsset(mainActivity.assets, "fonts/PLUMP.ttf")
            val row = TableRow(mainActivity)
            if (data[i] == "1") {
                rank.setTextColor(mainActivity.resources.getColor(Color.YELLOW))
                name.setTextColor(mainActivity.resources.getColor(Color.YELLOW))
                score.setTextColor(mainActivity.resources.getColor(Color.YELLOW))
            }
            if (data[i + 1] == mainActivity.username) {
                rank.setTextColor(Color.RED)
                name.setTextColor(Color.RED)
                score.setTextColor(Color.RED)
            }
            row.addView(rank)
            row.addView(name)
            row.addView(score)
            table.addView(row)
            i += 3
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