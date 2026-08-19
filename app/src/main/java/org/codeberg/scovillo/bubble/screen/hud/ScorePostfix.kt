package org.codeberg.scovillo.bubble.screen.hud

import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import org.codeberg.scovillo.bubble.MainActivity
import org.codeberg.scovillo.bubble.R

class ScorePostfix(private val mainActivity: MainActivity) {

    private val scoreTextPostfix: TextView = mainActivity.findViewById<View>(R.id.Score_Postfix) as TextView

    init {
        scoreTextPostfix.typeface = Typeface.createFromAsset(mainActivity.assets, "fonts/PLUMP.ttf")
    }

    fun animateWith(score: Int) {
        mainActivity.runOnUiThread {
            scoreTextPostfix.text = "+$score"
            scoreTextPostfix.alpha = 1.0f
            scoreTextPostfix.animate().alpha(0.0f).setDuration(400).setStartDelay(200).start()
        }
    }

}