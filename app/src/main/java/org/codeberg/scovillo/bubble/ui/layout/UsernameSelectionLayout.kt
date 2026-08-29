package org.codeberg.scovillo.bubble.ui.layout

import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import org.codeberg.scovillo.bubble.MainActivity
import org.codeberg.scovillo.bubble.R
import org.codeberg.scovillo.bubble.api.UserResource

class UsernameSelectionLayout(private val mainActivity: MainActivity) {

    fun showWith(users: MutableList<UserResource>) {
        mainActivity.setContentView(R.layout.username_selection)
        val usernameAdapter = UsernameListAdapter(mainActivity, users)
        val usernameListView = (mainActivity.findViewById<View>(R.id.username_list) as ListView)
        usernameListView.adapter = usernameAdapter
        usernameListView.onItemClickListener = OnItemClickListener { _, _, position, _ ->
            mainActivity.selectUser(users[position])
        }
    }

}
