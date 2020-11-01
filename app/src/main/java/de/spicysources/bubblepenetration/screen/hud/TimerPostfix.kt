package de.spicysources.bubblepenetration.screen.hud

import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import de.spicysources.bubblepenetration.MainActivity
import de.spicysources.bubblepenetration.R
import de.spicysources.bubblepenetration.util.roundToFirstDigit

class TimerPostfix(private val mainActivity: MainActivity) {

    private val timerTextPostfix: TextView = mainActivity.findViewById<View>(R.id.Timer_Postfix) as TextView

    init {
        timerTextPostfix.typeface = Typeface.createFromAsset(mainActivity.assets, "fonts/PLUMP.ttf")
    }

    fun animateWith(time: Float) {
        val formattedTime = roundToFirstDigit(time)
        mainActivity.runOnUiThread {
            timerTextPostfix.text = if (formattedTime < 0) "$formattedTime" else "+$formattedTime"
            timerTextPostfix.alpha = 1.0f
            timerTextPostfix.animate().alpha(0.0f).setDuration(400).setStartDelay(200).start()
        }
    }

}