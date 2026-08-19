package org.codeberg.scovillo.bubble.screen.layout

import android.graphics.Color.WHITE
import android.graphics.Color.YELLOW
import android.graphics.Typeface
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import org.codeberg.scovillo.bubble.MainActivity
import org.codeberg.scovillo.bubble.R
import org.codeberg.scovillo.bubble.data.ApiService
import org.codeberg.scovillo.bubble.THREAD_POOL
import java.util.concurrent.TimeUnit

class GameOverScreenLayout(private val mainActivity: MainActivity) {

    fun showWith(score: String) {
        mainActivity.setContentView(R.layout.game_over)

        setHighscoreResultAsync(score)

        (mainActivity.findViewById<View>(R.id.game_over_textview) as TextView).typeface = Typeface.createFromAsset(
            mainActivity.assets,
            "fonts/PLUMP.ttf"
        )
        (mainActivity.findViewById<View>(R.id.your_score_textview) as TextView).typeface = Typeface.createFromAsset(
            mainActivity.assets,
            "fonts/PLUMP.ttf"
        )
        (mainActivity.findViewById<View>(R.id.your_score_label) as TextView).typeface = Typeface.createFromAsset(
            mainActivity.assets,
            "fonts/PLUMP.ttf"
        )
        (mainActivity.findViewById<View>(R.id.highscore_gameover) as Button).typeface = Typeface.createFromAsset(
            mainActivity.assets,
            "fonts/PLUMP.ttf"
        )
        (mainActivity.findViewById<View>(R.id.bewerten_button) as Button).typeface = Typeface.createFromAsset(
            mainActivity.assets,
            "fonts/PLUMP.ttf"
        )
        (mainActivity.findViewById<View>(R.id.support_us) as Button).typeface = Typeface.createFromAsset(
            mainActivity.assets,
            "fonts/PLUMP.ttf"
        )
        (mainActivity.findViewById<View>(R.id.highscore_label) as TextView).typeface = Typeface.createFromAsset(
            mainActivity.assets,
            "fonts/PLUMP.ttf"
        )
    }

    private fun setHighscoreResultAsync(score: String) {
        val gameOverScore = mainActivity.findViewById<View>(R.id.your_score_textview) as TextView
        gameOverScore.text = score
        THREAD_POOL.execute {
            try {
                val matchEndResource = ApiService.endMatch(mainActivity.currentMatch.id, score.toInt())[6000, TimeUnit.MILLISECONDS]
                mainActivity.runOnUiThread {
                    if (matchEndResource.isHighscore) {
                        (mainActivity.findViewById<View>(R.id.highscore_label) as TextView).text = "Great! check your new rank!"
                        (mainActivity.findViewById<View>(R.id.your_score_label) as TextView).text = "!!! New Highscore !!!"
                        (mainActivity.findViewById<View>(R.id.your_score_label) as TextView).setTextColor(YELLOW)
                    } else {
                        (mainActivity.findViewById<View>(R.id.highscore_label) as TextView).text = "you were better...try again!"
                        (mainActivity.findViewById<View>(R.id.your_score_label) as TextView).text = "Your score"
                        (mainActivity.findViewById<View>(R.id.your_score_label) as TextView).setTextColor(WHITE)
                    }
                }
            } catch (exception: Exception) {
                mainActivity.runOnUiThread {
                    (mainActivity.findViewById<View>(R.id.highscore_label) as TextView).text = "Server is currently not available...please try again later!"
                    (mainActivity.findViewById<View>(R.id.your_score_label) as TextView).text = "Your score"
                    (mainActivity.findViewById<View>(R.id.your_score_label) as TextView).setTextColor(WHITE)
                    Toast.makeText(mainActivity, "Server is currently not available...please try again later!", LENGTH_LONG).show()
                }
            }
        }
    }

}