package org.codeberg.scovillo.bubble.ui.layout

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.codeberg.scovillo.bubble.MainActivity
import org.codeberg.scovillo.bubble.R
import org.codeberg.scovillo.bubble.THREAD_POOL
import org.codeberg.scovillo.bubble.api.ApiService
import org.codeberg.scovillo.bubble.ui.BubbleFont
import java.util.concurrent.TimeUnit
import kotlin.math.max

class HighscoreLayout(private val mainActivity: MainActivity) {

    private data class HighscoreRow(val rank: Int, val username: String, val score: String)

    private var firstRank = 1
    private var lastRank = 0
    private var hasPrevious = false
    private var hasNext = false
    private var loading = false

    fun show() {
        mainActivity.setContentView(R.layout.highscores)
        firstRank = 1
        lastRank = 0
        hasPrevious = false
        hasNext = false
        loading = false
        loadPage(username = mainActivity.selectedUser.username)
    }

    private fun generateHighscoreTextView(): TextView {
        val tv = TextView(mainActivity)
        tv.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT,
            0.25f
        )
        tv.gravity = 1
        tv.setTextColor(Color.WHITE)
        tv.textSize = 25f
        return tv
    }

    private fun generateEmptyHighscoreView(): View {
        val density = mainActivity.resources.displayMetrics.density
        val message = mainActivity.getString(R.string.empty_highscores)
            .removePrefix("🏆")
            .trimStart()
        return LinearLayout(mainActivity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, (40 * density).toInt(), 0, 0)
            addView(TextView(mainActivity).apply {
                text = "🏆"
                gravity = Gravity.CENTER
                textSize = 72f
                setTextColor(ContextCompat.getColor(mainActivity, R.color.gold))
            })
            addView(generateHighscoreTextView().apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (24 * density).toInt()
                }
                text = message
                BubbleFont.applyTo(this, scaleNonButtonText = false)
            })
        }
    }

    private fun loadPage(
        username: String? = null,
        startRank: Int? = null,
        prepend: Boolean = false
    ) {
        if (!mainActivity.settingsModel.useOnlineLeaderboard) {
            addLocalHighscores()
            return
        }
        if (loading) return
        loading = true
        THREAD_POOL.execute {
            try {
                val response =
                    ApiService.getHighscorePage(username, startRank)[8000, TimeUnit.MILLISECONDS]
                val jsonArray = response.getJSONArray("highscores")
                mainActivity.onBackendRequestSucceeded()
                val rows = (0 until jsonArray.length()).map { index ->
                    val item = jsonArray.getJSONObject(index)
                    HighscoreRow(
                        item.getInt("rank"),
                        item.getString("username"),
                        item.getString("score")
                    )
                }
                showPage(
                    rows,
                    response.getBoolean("hasPrevious"),
                    response.getBoolean("hasNext"),
                    prepend
                )
            } catch (exception: Exception) {
                mainActivity.runOnUiThread {
                    loading = false
                    if (lastRank == 0) {
                        addLocalHighscores()
                        if (!mainActivity.showRateLimitMessage(exception)) {
                            mainActivity.showOfflineFallbackMessageOnce()
                        }
                    }
                }
                exception.printStackTrace()
            }
        }
    }

    private fun showPage(
        rows: List<HighscoreRow>,
        pageHasPrevious: Boolean,
        pageHasNext: Boolean,
        prepend: Boolean
    ) {
        mainActivity.runOnUiThread {
            val table = mainActivity.findViewById<TableLayout?>(R.id.highscore_table)
                ?: return@runOnUiThread
            val scroll = mainActivity.findViewById<ScrollView?>(R.id.highscore_scroll)
                ?: return@runOnUiThread
            val newRows = rows.filter { it.rank !in firstRank..lastRank }
            val oldHeight = table.height
            if (rows.isEmpty() && table.childCount == 0) {
                table.addView(generateEmptyHighscoreView())
            }
            val renderedRows = newRows.map { row ->
                TableRow(mainActivity).apply {
                    val rank = generateHighscoreTextView().apply {
                        text = mainActivity.getString(R.string.rank_value, row.rank)
                        BubbleFont.applyTo(this, scaleNonButtonText = false)
                    }
                    val name = generateHighscoreTextView().apply {
                        text = row.username
                        BubbleFont.applyTo(this, scaleNonButtonText = false)
                    }
                    val score = generateHighscoreTextView().apply {
                        text = row.score
                        BubbleFont.applyTo(this, scaleNonButtonText = false)
                    }
                    if (row.username == mainActivity.selectedUser.username) {
                        rank.setTextColor(Color.YELLOW)
                        name.setTextColor(Color.YELLOW)
                        score.setTextColor(Color.YELLOW)
                    }
                    addView(rank)
                    addView(name)
                    addView(score)
                }
            }
            if (prepend) {
                renderedRows.asReversed().forEach { table.addView(it, 0) }
                hasPrevious = pageHasPrevious
            } else {
                renderedRows.forEach(table::addView)
                hasNext = pageHasNext
            }
            if (rows.isNotEmpty()) {
                firstRank =
                    minOf(firstRank.takeIf { lastRank > 0 } ?: rows.first().rank, rows.first().rank)
                lastRank = maxOf(lastRank, rows.last().rank)
            }
            loading = false
            table.post {
                if (prepend) {
                    scroll.scrollTo(0, scroll.scrollY + table.height - oldHeight)
                } else if (lastRank == rows.lastOrNull()?.rank && firstRank == rows.firstOrNull()?.rank) {
                    scroll.scrollTo(0, if (hasPrevious) 1 else 0)
                    installPagination(scroll)
                }
            }
        }
    }

    private fun installPagination(scroll: ScrollView) {
        val oldScrollY = scroll.scrollY
        scroll.viewTreeObserver.addOnScrollChangedListener scrollChanged@{
            val scrollY = scroll.scrollY
            val child = scroll.getChildAt(0) ?: return@scrollChanged
            when {
                scrollY < oldScrollY && scrollY == 0 && hasPrevious ->
                    loadPage(startRank = max(1, firstRank - 50), prepend = true)

                scrollY > oldScrollY && child.bottom <= scroll.height + scrollY && hasNext ->
                    loadPage(startRank = lastRank + 1)
            }
        }
    }

    private fun addLocalHighscores() {
        val rows = mainActivity.localHighscoreStorage.read().mapIndexed { index, highscore ->
            HighscoreRow(index + 1, highscore.username, highscore.score.toString())
        }
        showPage(rows, pageHasPrevious = false, pageHasNext = false, prepend = false)
    }

}
