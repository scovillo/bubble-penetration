package org.codeberg.scovillo.bubble.ui.layout

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.codeberg.scovillo.bubble.MainActivity
import org.codeberg.scovillo.bubble.R
import org.codeberg.scovillo.bubble.THREAD_POOL
import org.codeberg.scovillo.bubble.api.ApiService
import java.util.concurrent.TimeUnit
import kotlin.math.max

class HighscoreLayout(private val mainActivity: MainActivity) {

    private companion object {
        const val PAGE_SIZE = 50
        const val LOAD_THRESHOLD = 5
    }

    private var rows = emptyList<HighscoreRow>()
    private var hasPrevious = false
    private var hasNext = false
    private var loading = false
    private var requestGeneration = 0

    private lateinit var adapter: HighscoreListAdapter
    private lateinit var layoutManager: LinearLayoutManager

    fun show() {
        mainActivity.setContentView(R.layout.highscores)
        requestGeneration++
        rows = emptyList()
        hasPrevious = false
        hasNext = false
        loading = false

        adapter = HighscoreListAdapter(mainActivity.selectedUser.username)
        layoutManager = LinearLayoutManager(mainActivity)
        mainActivity.findViewById<RecyclerView>(R.id.highscore_list).apply {
            layoutManager = this@HighscoreLayout.layoutManager
            adapter = this@HighscoreLayout.adapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val firstVisible =
                        this@HighscoreLayout.layoutManager.findFirstVisibleItemPosition()
                    val lastVisible =
                        this@HighscoreLayout.layoutManager.findLastVisibleItemPosition()
                    if (
                        dy < 0 &&
                        firstVisible != RecyclerView.NO_POSITION &&
                        firstVisible <= LOAD_THRESHOLD
                    ) {
                        loadPreviousPage()
                    } else if (
                        dy > 0 &&
                        lastVisible != RecyclerView.NO_POSITION &&
                        lastVisible >=
                        this@HighscoreLayout.adapter.itemCount - 1 - LOAD_THRESHOLD
                    ) {
                        loadNextPage()
                    }
                }
            })
        }

        loadPage(username = mainActivity.selectedUser.username)
    }

    private fun loadPreviousPage() {
        if (!hasPrevious || rows.isEmpty()) return
        loadPage(startRank = max(1, rows.first().rank - PAGE_SIZE), prepend = true)
    }

    private fun loadNextPage() {
        if (!hasNext || rows.isEmpty()) return
        loadPage(startRank = rows.last().rank + 1)
    }

    private fun loadPage(
        username: String? = null,
        startRank: Int? = null,
        prepend: Boolean = false,
    ) {
        if (!mainActivity.settingsModel.useOnlineLeaderboard) {
            addLocalHighscores()
            return
        }
        if (loading) return
        loading = true
        val generation = requestGeneration
        THREAD_POOL.execute {
            try {
                val response =
                    ApiService.getHighscorePage(username, startRank)[8000, TimeUnit.MILLISECONDS]
                val jsonArray = response.getJSONArray("highscores")
                val pageRows = (0 until jsonArray.length()).map { index ->
                    val item = jsonArray.getJSONObject(index)
                    HighscoreRow(
                        item.getInt("rank"),
                        item.getString("username"),
                        item.getString("score"),
                    )
                }
                mainActivity.onBackendRequestSucceeded()
                mainActivity.runOnUiThread {
                    if (generation != requestGeneration) return@runOnUiThread
                    showPage(
                        pageRows,
                        response.getBoolean("hasPrevious"),
                        response.getBoolean("hasNext"),
                        prepend,
                    )
                }
            } catch (exception: Exception) {
                mainActivity.runOnUiThread {
                    if (generation != requestGeneration) return@runOnUiThread
                    loading = false
                    if (rows.isEmpty()) {
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
        pageRows: List<HighscoreRow>,
        pageHasPrevious: Boolean,
        pageHasNext: Boolean,
        prepend: Boolean,
    ) {
        val recycler = mainActivity.findViewById<RecyclerView?>(R.id.highscore_list) ?: return
        val isInitialPage = rows.isEmpty()
        val anchorPosition = if (prepend) layoutManager.findFirstVisibleItemPosition() else -1
        val anchorUsername = rows.getOrNull(anchorPosition)?.username
        val anchorOffset = if (anchorPosition != RecyclerView.NO_POSITION) {
            layoutManager.findViewByPosition(anchorPosition)?.let(layoutManager::getDecoratedTop) ?: 0
        } else {
            0
        }

        val knownRanks = rows.asSequence().mapTo(mutableSetOf()) { it.rank }
        val knownUsernames = rows.asSequence().mapTo(mutableSetOf()) { it.username }
        val newRows = pageRows.filter {
            if (it.rank in knownRanks || it.username in knownUsernames) {
                false
            } else {
                knownRanks.add(it.rank)
                knownUsernames.add(it.username)
                true
            }
        }
        val updatedRows = if (prepend) newRows + rows else rows + newRows
        rows = updatedRows.sortedBy { it.rank }
        if (isInitialPage) {
            hasPrevious = pageHasPrevious
            hasNext = pageHasNext
        } else if (prepend) {
            hasPrevious = pageHasPrevious
        } else {
            hasNext = pageHasNext
        }
        if (pageRows.isEmpty()) {
            if (prepend) hasPrevious = false else hasNext = false
        }
        mainActivity.findViewById<View>(R.id.empty_highscores_view).visibility =
            if (rows.isEmpty()) View.VISIBLE else View.GONE
        recycler.visibility = if (rows.isEmpty()) View.GONE else View.VISIBLE
        adapter.submitList(rows) {
            if (isInitialPage) {
                centerSelectedUser(recycler)
            } else if (
                prepend &&
                anchorPosition != RecyclerView.NO_POSITION &&
                anchorUsername != null
            ) {
                val updatedAnchorPosition = rows.indexOfFirst { it.username == anchorUsername }
                layoutManager.scrollToPositionWithOffset(
                    updatedAnchorPosition.takeIf { it != -1 } ?: anchorPosition,
                    anchorOffset,
                )
            }
            loading = false
        }
    }

    private fun centerSelectedUser(recycler: RecyclerView) {
        val selectedUserPosition = rows.indexOfFirst {
            it.username == mainActivity.selectedUser.username
        }
        if (selectedUserPosition == -1) return

        recycler.post {
            val rowHeight = recycler.getChildAt(0)?.height ?: 0
            val contentHeight = recycler.height - recycler.paddingTop - recycler.paddingBottom
            val centeredOffset = recycler.paddingTop + max(0, (contentHeight - rowHeight) / 2)
            layoutManager.scrollToPositionWithOffset(selectedUserPosition, centeredOffset)
        }
    }

    private fun addLocalHighscores() {
        val localRows = mainActivity.localHighscoreStorage.read().mapIndexed { index, highscore ->
            HighscoreRow(index + 1, highscore.username, highscore.score.toString())
        }
        showPage(localRows, pageHasPrevious = false, pageHasNext = false, prepend = false)
    }
}
