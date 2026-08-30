package org.codeberg.scovillo.bubble.ui.layout

import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import org.codeberg.scovillo.bubble.MainActivity
import org.codeberg.scovillo.bubble.R
import org.codeberg.scovillo.bubble.ui.BubbleFont
import org.codeberg.scovillo.bubble.api.UserResource

class UsernameListAdapter(private val mainActivity: MainActivity, private val users: MutableList<UserResource>) : ArrayAdapter<UserResource>(mainActivity, R.layout.username_list_row, users) {

    private var inflater: LayoutInflater = mainActivity.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = convertView ?: inflater.inflate(R.layout.username_list_row, parent, false).also {
            BubbleFont.applyTo(it)
        }
        val usernameRowTextView = row.findViewById<View>(R.id.username_row_text) as TextView
        usernameRowTextView.text = users[position].username
        return row
    }

}
