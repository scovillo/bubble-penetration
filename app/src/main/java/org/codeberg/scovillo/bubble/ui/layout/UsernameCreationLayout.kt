package org.codeberg.scovillo.bubble.ui.layout

import android.view.View
import org.codeberg.scovillo.bubble.MainActivity
import org.codeberg.scovillo.bubble.R

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

    }

}
