package de.spicysources.bubblepenetration.screen.layout

import android.graphics.Typeface
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import de.spicysources.bubblepenetration.MainActivity
import de.spicysources.bubblepenetration.R

class UsernameCreationLayout(private val mainActivity: MainActivity) {

    fun showWith(isCancelEnabled: Boolean) {
        mainActivity.setContentView(R.layout.username_creation)

        if (isCancelEnabled) {
            mainActivity.findViewById<View>(R.id.cancel).isEnabled = true
            mainActivity.findViewById<View>(R.id.cancel).visibility = View.VISIBLE
        } else {
            mainActivity.findViewById<View>(R.id.cancel).isEnabled = false
            mainActivity.findViewById<View>(R.id.cancel).visibility = View.INVISIBLE
        }

        (mainActivity.findViewById<View>(R.id.your_name) as TextView).typeface = Typeface.createFromAsset(
            mainActivity.assets,
            "fonts/PLUMP.ttf"
        )
        (mainActivity.findViewById<View>(R.id.save) as Button).typeface = Typeface.createFromAsset(
            mainActivity.assets,
            "fonts/PLUMP.ttf"
        )
        (mainActivity.findViewById<View>(R.id.cancel) as Button).typeface = Typeface.createFromAsset(
            mainActivity.assets,
            "fonts/PLUMP.ttf"
        )
        (mainActivity.findViewById<View>(R.id.username_field) as EditText).typeface = Typeface.createFromAsset(
            mainActivity.assets,
            "fonts/PLUMP.ttf"
        )
    }

}