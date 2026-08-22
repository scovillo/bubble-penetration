package org.codeberg.scovillo.bubble.ui.layout

import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.*
import org.codeberg.scovillo.bubble.MainActivity
import org.codeberg.scovillo.bubble.R
import org.codeberg.scovillo.bubble.api.ApiService
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
        if (!mainActivity.settingsModel.useOnlineLeaderboard) {
            addHighscores(mainActivity.localHighscoreStorage.read().map { it.username to it.score.toString() })
            return
        }
        THREAD_POOL.execute {
            try {
                val highscoreRequest = ApiService.getHighscoreData()
                val jsonArray = highscoreRequest[8000, TimeUnit.MILLISECONDS]
                mainActivity.onBackendRequestSucceeded()

                addHighscores((0 until jsonArray.length()).map { index ->
                    jsonArray.getJSONObject(index).getString("username") to
                        jsonArray.getJSONObject(index).getString("score")
                })
            } catch (exception: Exception) {
                mainActivity.runOnUiThread {
                    addHighscores(mainActivity.localHighscoreStorage.read().map { it.username to it.score.toString() })
                    mainActivity.showOfflineFallbackMessageOnce()
                }
                exception.printStackTrace()
            }
        }
    }

    private fun addHighscores(highscores: List<Pair<String, String>>) {
        mainActivity.runOnUiThread {
            val table = mainActivity.findViewById<View>(R.id.highscore_table) as TableLayout
            highscores.forEachIndexed { index, (username, scoreValue) ->
                val rank = generateHighscoreTextView().apply {
                    text = "${index + 1}"
                    typeface = Typeface.createFromAsset(mainActivity.assets, "fonts/PLUMP.ttf")
                }
                val name = generateHighscoreTextView().apply {
                    text = username
                    typeface = Typeface.createFromAsset(mainActivity.assets, "fonts/PLUMP.ttf")
                }
                val score = generateHighscoreTextView().apply {
                    text = scoreValue
                    typeface = Typeface.createFromAsset(mainActivity.assets, "fonts/PLUMP.ttf")
                }
                if (username == mainActivity.selectedUser.username) {
                    rank.setTextColor(Color.YELLOW)
                    name.setTextColor(Color.YELLOW)
                    score.setTextColor(Color.YELLOW)
                }
                table.addView(TableRow(mainActivity).apply {
                    addView(rank)
                    addView(name)
                    addView(score)
                })
            }
        }
    }

}
