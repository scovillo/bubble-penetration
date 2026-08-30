package org.codeberg.scovillo.bubble.ui.layout

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.codeberg.scovillo.bubble.R
import org.codeberg.scovillo.bubble.ui.BubbleFont

data class HighscoreRow(
    val rank: Int,
    val username: String,
    val score: String,
)

class HighscoreListAdapter(
    private val selectedUsername: String,
) : ListAdapter<HighscoreRow, HighscoreListAdapter.HighscoreViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HighscoreViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.highscore_list_row, parent, false)
        return HighscoreViewHolder(view)
    }

    override fun onBindViewHolder(holder: HighscoreViewHolder, position: Int) {
        holder.bind(getItem(position), selectedUsername)
    }

    class HighscoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rank = itemView.findViewById<TextView>(R.id.highscore_rank)
        private val username = itemView.findViewById<TextView>(R.id.highscore_username)
        private val score = itemView.findViewById<TextView>(R.id.highscore_score)
        private val defaultTextColor = rank.currentTextColor

        init {
            BubbleFont.applyTo(itemView, scaleNonButtonText = false)
        }

        fun bind(row: HighscoreRow, selectedUsername: String) {
            rank.text = itemView.context.getString(R.string.rank_value, row.rank)
            username.text = row.username
            score.text = row.score
            val textColor = if (row.username == selectedUsername) Color.YELLOW else defaultTextColor
            rank.setTextColor(textColor)
            username.setTextColor(textColor)
            score.setTextColor(textColor)
        }
    }

    private companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<HighscoreRow>() {
            override fun areItemsTheSame(oldItem: HighscoreRow, newItem: HighscoreRow): Boolean =
                oldItem.username == newItem.username

            override fun areContentsTheSame(oldItem: HighscoreRow, newItem: HighscoreRow): Boolean =
                oldItem == newItem
        }
    }
}
