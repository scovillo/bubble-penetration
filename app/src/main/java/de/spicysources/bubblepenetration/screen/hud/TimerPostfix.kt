package de.spicysources.bubblepenetration.screen.hud

import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import de.spicysources.bubblepenetration.MainActivity
import de.spicysources.bubblepenetration.R
import java.math.RoundingMode.CEILING
import java.text.DecimalFormat

class TimerPostfix(private val mainActivity: MainActivity) {

    private val timerTextPostfix: TextView = mainActivity.findViewById<View>(R.id.Timer_Postfix) as TextView

    private val firstDigitFormat = DecimalFormat("0.0")

    init {
        timerTextPostfix.typeface = Typeface.createFromAsset(mainActivity.assets, "fonts/PLUMP.ttf")
        firstDigitFormat.roundingMode = CEILING
    }

    fun animateWith(time: Float) {
        val formattedTime = firstDigitFormat.format(time)
        mainActivity.runOnUiThread {
            timerTextPostfix.text = if (time < 0) "$formattedTime" else "+$formattedTime"
            timerTextPostfix.alpha = 1.0f
            timerTextPostfix.animate().alpha(0.0f).setDuration(400).setStartDelay(200).start()
        }
    }

}