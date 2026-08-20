package org.codeberg.scovillo.bubble.ui.layout

import android.graphics.Typeface
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import org.codeberg.scovillo.bubble.MainActivity
import org.codeberg.scovillo.bubble.R
import org.codeberg.scovillo.bubble.api.UserResource

class UsernameSelectionLayout(private val mainActivity: MainActivity) {

    fun showWith(users: MutableList<UserResource>) {
        mainActivity.setContentView(R.layout.username_selection)
        (mainActivity.findViewById<View>(R.id.greeting) as TextView).typeface = Typeface.createFromAsset(
            mainActivity.assets,
            "fonts/PLUMP.ttf"
        )
        (mainActivity.findViewById<View>(R.id.username_creation_button) as Button).typeface = Typeface.createFromAsset(
            mainActivity.assets,
            "fonts/PLUMP.ttf"
        )
        val usernameAdapter = UsernameListAdapter(mainActivity, users)
        val usernameListView = (mainActivity.findViewById<View>(R.id.username_list) as ListView)
        usernameListView.adapter = usernameAdapter
        usernameListView.onItemClickListener = OnItemClickListener { _, _, position, _ ->
            mainActivity.selectUser(users[position])
        }
    }

}