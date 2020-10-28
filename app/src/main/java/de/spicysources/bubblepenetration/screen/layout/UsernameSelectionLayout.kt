package de.spicysources.bubblepenetration.screen.layout

import android.graphics.Typeface
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import de.spicysources.bubblepenetration.MainActivity
import de.spicysources.bubblepenetration.R

class UsernameSelectionLayout(private val mainActivity: MainActivity) {

    fun showWith(usernames: MutableList<String>) {
        mainActivity.setContentView(R.layout.username_selection)
        (mainActivity.findViewById<View>(R.id.greeting) as TextView).typeface = Typeface.createFromAsset(
            mainActivity.assets,
            "fonts/PLUMP.ttf"
        )
        (mainActivity.findViewById<View>(R.id.username_creation_button) as Button).typeface = Typeface.createFromAsset(
            mainActivity.assets,
            "fonts/PLUMP.ttf"
        )
        val usernameAdapter = UsernameListAdapter(mainActivity, usernames)
        val usernameListView = (mainActivity.findViewById<View>(R.id.username_list) as ListView)
        usernameListView.adapter = usernameAdapter
        usernameListView.onItemClickListener = OnItemClickListener { _, _, position, _ ->
            val selectedUsername = usernameListView.getItemAtPosition(position) as String
            mainActivity.selectUsername(selectedUsername)
        }
    }

}