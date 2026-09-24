package org.codeberg.scovillo.bubble.android.ui.hud

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.codeberg.scovillo.bubble.R
import org.codeberg.scovillo.bubble.android.ui.BubbleFont

/** Shows the finish cue before the result screen replaces the match HUD. */
class MatchFinishOverlay(
    private val context: Context,
    private val holder: FrameLayout,
) {
    fun show(onFinished: () -> Unit) {
        val text = TextView(context).apply {
            gravity = Gravity.CENTER
            setText(R.string.match_finished)
            setTextColor(ContextCompat.getColor(context, R.color.gold))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE_SP)
            setShadowLayer(8f, 0f, 3f, Color.BLACK)
            BubbleFont.applyTo(this, scaleNonButtonText = false)
        }
        holder.addView(
            text,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        text.postDelayed(onFinished, DISPLAY_DURATION_MS)
    }

    private companion object {
        const val DISPLAY_DURATION_MS = 1_000L
        const val TEXT_SIZE_SP = 64f
    }
}
