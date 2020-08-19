package de.spicysources.bubblepenetration.screen.layout

import android.graphics.Color.WHITE
import android.graphics.Color.YELLOW
import android.graphics.Typeface
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import de.spicysources.bubblepenetration.MainActivity
import de.spicysources.bubblepenetration.R
import de.spicysources.bubblepenetration.data.DataConnection
import de.spicysources.bubblepenetration.objects.GameObject
import java.util.concurrent.CompletableFuture

class GameOverScreenLayout(private val mainActivity: MainActivity) {

    fun showWith(score: String) {
        GameObject.speed = 1.0f
        mainActivity.setContentView(R.layout.game_over)
        val gameOverScore = mainActivity.findViewById<View>(R.id.your_score_textview) as TextView
        gameOverScore.text = score
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

        val better: Boolean? = CompletableFuture.supplyAsync {
            DataConnection.registerHighscore(mainActivity.username, score)
        }.exceptionally { null }.join()

        if (better == null) {
            Toast.makeText(mainActivity, "Server is currently not available...please try again later.", 1500).show()
            return
        }
        if (better) {
            (mainActivity.findViewById<View>(R.id.highscore_label) as TextView).text = "Great! check your new rank!"
            (mainActivity.findViewById<View>(R.id.your_score_label) as TextView).text = "!!! New Highscore !!!"
            (mainActivity.findViewById<View>(R.id.your_score_label) as TextView).setTextColor(YELLOW)
        } else {
            (mainActivity.findViewById<View>(R.id.highscore_label) as TextView).text = "you were better...try again!"
            (mainActivity.findViewById<View>(R.id.your_score_label) as TextView).text = "Your score"
            (mainActivity.findViewById<View>(R.id.your_score_label) as TextView).setTextColor(WHITE)
        }
        (mainActivity.findViewById<View>(R.id.highscore_label) as TextView).typeface = Typeface.createFromAsset(
            mainActivity.assets,
            "fonts/PLUMP.ttf"
        )
    }

}