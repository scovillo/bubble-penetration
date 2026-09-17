package org.codeberg.scovillo.bubble.android.ui.layout

import android.view.View
import org.codeberg.scovillo.bubble.R
import org.codeberg.scovillo.bubble.android.MainActivity

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
