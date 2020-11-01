package de.spicysources.bubblepenetration.logic

import android.graphics.Typeface
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import de.spicysources.bubblepenetration.MainActivity
import de.spicysources.bubblepenetration.R
import java.lang.System.currentTimeMillis

class Combo(private val mainActivity: MainActivity) {

    val multiplikator
    get() = run {
        val factor = counter / comboCollectFactor
        if (factor > 0) factor + factor else 1
    }

    private val isActive
        get() = multiplikator > 1

    private var counter = 0

    private val comboCollectFactor = 6

    private val duration = 2500

    private var lastBubble = currentTimeMillis()

    private var textView = mainActivity.findViewById<View>(R.id.Combo) as TextView

    private val textAnimation: Animation = AlphaAnimation(0.35f, 1.0f)

    private val symbol = "●"

    init {
        textView.typeface = Typeface.createFromAsset(mainActivity.assets, "fonts/PLUMP.ttf")
        textAnimation.duration = 300
        textAnimation.startOffset = 20
        textAnimation.repeatMode = Animation.REVERSE
        textAnimation.repeatCount = Animation.INFINITE
    }

    fun update() {
        if (currentTimeMillis() > lastBubble + duration) {
            reset()
        }
        mainActivity.runOnUiThread {
            var comboProgressText = ""

            if (isActive) {
                comboProgressText = "Combo x$multiplikator "
                if (textView.animation == null) {
                    textView.startAnimation(textAnimation)
                }
            } else {
                textView.animation?.cancel()
            }
            for (i in 0 until counter%comboCollectFactor) {
                comboProgressText = "$comboProgressText$symbol"
            }
            textView.text = comboProgressText
        }
    }

    fun increment() {
        counter++
        lastBubble = currentTimeMillis()
    }

    fun reset() {
        counter = 0
    }

}