package org.codeberg.scovillo.bubble.ui.layout

import android.graphics.Color.WHITE
import android.graphics.Color.YELLOW
import android.graphics.Typeface
import android.widget.Button
import android.widget.TextView
import org.codeberg.scovillo.bubble.MainActivity
import org.codeberg.scovillo.bubble.R
import org.codeberg.scovillo.bubble.THREAD_POOL
import org.codeberg.scovillo.bubble.api.ApiService
import java.util.concurrent.TimeUnit

class GameOverScreenLayout(private val mainActivity: MainActivity) {

    fun showWith(score: String) {
        mainActivity.setContentView(R.layout.game_over)

        setHighscoreResultAsync(score)

        val font = Typeface.createFromAsset(mainActivity.assets, "fonts/PLUMP.ttf")

        mainActivity.findViewById<TextView>(R.id.game_over_textview)?.typeface = font
        mainActivity.findViewById<TextView>(R.id.your_score_textview)?.typeface = font
        mainActivity.findViewById<TextView>(R.id.your_score_label)?.typeface = font
        mainActivity.findViewById<Button>(R.id.play_again_button)?.typeface = font
        mainActivity.findViewById<Button>(R.id.highscore_gameover)?.typeface = font
        mainActivity.findViewById<Button>(R.id.rating_button)?.typeface = font
        mainActivity.findViewById<Button>(R.id.project_button)?.typeface = font
        mainActivity.findViewById<TextView>(R.id.highscore_label)?.typeface = font
    }

    private fun setHighscoreResultAsync(score: String) {
        val gameOverScore = mainActivity.findViewById<TextView>(R.id.your_score_textview)
        gameOverScore?.text = score
        if (!mainActivity.settingsModel.useOnlineLeaderboard) {
            val isRecord = mainActivity.localHighscoreStorage.saveIfHigher(
                mainActivity.selectedUser.username,
                score.toInt(),
            )
            updateHighscoreResult(isRecord)
            return
        }
        THREAD_POOL.execute {
            try {
                val isRecord = ApiService.registerHighscore(mainActivity.selectedUser.username, score)[6000, TimeUnit.MILLISECONDS]
                mainActivity.runOnUiThread {
                    mainActivity.onBackendRequestSucceeded()
                    updateHighscoreResult(isRecord)
                }
            } catch (exception: Exception) {
                mainActivity.runOnUiThread {
                    val isRecord = mainActivity.localHighscoreStorage.saveIfHigher(
                        mainActivity.selectedUser.username,
                        score.toInt(),
                    )
                    updateHighscoreResult(isRecord)
                    if (!mainActivity.showRateLimitMessage(exception)) {
                        mainActivity.showOfflineFallbackMessageOnce()
                    }
                }
                exception.printStackTrace()
            }
        }
    }

    private fun updateHighscoreResult(isRecord: Boolean) {
        val highscoreLabel = mainActivity.findViewById<TextView>(R.id.highscore_label)
        val yourScoreLabel = mainActivity.findViewById<TextView>(R.id.your_score_label)
        if (isRecord) {
            highscoreLabel?.text = mainActivity.getString(R.string.msg_new_rank)
            yourScoreLabel?.text = mainActivity.getString(R.string.new_highscore)
            yourScoreLabel?.setTextColor(YELLOW)
        } else {
            highscoreLabel?.text = mainActivity.getString(R.string.msg_try_again)
            yourScoreLabel?.text = mainActivity.getString(R.string.msg_your_score)
            yourScoreLabel?.setTextColor(WHITE)
        }
    }

}
