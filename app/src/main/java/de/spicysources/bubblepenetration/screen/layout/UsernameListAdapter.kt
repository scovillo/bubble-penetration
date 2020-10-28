package de.spicysources.bubblepenetration.screen.layout

import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import de.spicysources.bubblepenetration.MainActivity
import de.spicysources.bubblepenetration.R

class UsernameListAdapter(private val mainActivity: MainActivity, private val usernames: MutableList<String>) : ArrayAdapter<String>(mainActivity, R.layout.username_list_row, usernames) {

    private var inflater: LayoutInflater = mainActivity.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var row = convertView
        if (convertView == null) row = inflater.inflate(R.layout.username_list_row, null, true)
        row!!
        val usernameRowTextView = row.findViewById<View>(R.id.username_row_text) as TextView
        usernameRowTextView.text = usernames[position]
        usernameRowTextView.typeface = Typeface.createFromAsset(mainActivity.assets, "fonts/PLUMP.ttf")
        return row
    }

}