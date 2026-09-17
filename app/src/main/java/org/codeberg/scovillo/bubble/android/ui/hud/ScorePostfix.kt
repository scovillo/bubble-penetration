package org.codeberg.scovillo.bubble.android.ui.hud

import android.view.View
import android.widget.TextView
import org.codeberg.scovillo.bubble.R
import org.codeberg.scovillo.bubble.android.MainActivity

class ScorePostfix(private val mainActivity: MainActivity) {

    private val scoreTextPostfix: TextView =
        mainActivity.findViewById<View>(R.id.Score_Postfix) as TextView

    init {
    }

    fun animateWith(score: Int) {
        mainActivity.runOnUiThread {
            scoreTextPostfix.text = mainActivity.getString(R.string.score_postfix_value, score)
            scoreTextPostfix.alpha = 1.0f
            scoreTextPostfix.animate().alpha(0.0f).setDuration(400).setStartDelay(200).start()
        }
    }

}
