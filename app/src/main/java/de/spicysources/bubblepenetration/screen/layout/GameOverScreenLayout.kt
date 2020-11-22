package de.spicysources.bubblepenetration.screen.layout

import android.graphics.Color.WHITE
import android.graphics.Color.YELLOW
import android.graphics.Typeface
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import de.spicysources.bubblepenetration.MainActivity
import de.spicysources.bubblepenetration.R
import de.spicysources.bubblepenetration.data.DataConnection
import de.spicysources.bubblepenetration.THREAD_POOL
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
    }

    private fun setHighscoreResultAsync(score: String) {
        val gameOverScore = mainActivity.findViewById<View>(R.id.your_score_textview) as TextView
        gameOverScore.text = score
        THREAD_POOL.execute {
            try {
                val better = DataConnection.registerHighscore(mainActivity.selectedUsername, score)[6000, TimeUnit.MILLISECONDS]
                mainActivity.runOnUiThread {
                    if (better) {
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
            (mainActivity.findViewById<View>(R.id.highscore_label) as TextView).typeface = Typeface.createFromAsset(
                mainActivity.assets,
                "fonts/PLUMP.ttf"
            )
        }
    }

}