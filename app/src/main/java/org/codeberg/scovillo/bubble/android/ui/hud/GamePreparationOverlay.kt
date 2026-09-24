package org.codeberg.scovillo.bubble.android.ui.hud

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.codeberg.scovillo.bubble.R
import org.codeberg.scovillo.bubble.android.ui.BubbleFont

/** Shows the short, non-interactive preparation sequence before a match begins. */
class GamePreparationOverlay(
    private val context: Context,
    private val holder: FrameLayout,
) {
    fun show(isActive: () -> Boolean, onFinished: () -> Unit) {
        val text = TextView(context).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = READY_TEXT_SIZE_SP
            setShadowLayer(8f, 0f, 3f, Color.BLACK)
            BubbleFont.applyTo(this, scaleNonButtonText = false)
            setText(R.string.preparation_ready)
        }
        holder.addView(
            text,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        text.postDelayed({
            if (isActive()) text.setText(R.string.preparation_collect) else holder.removeView(text)
        }, READY_DURATION_MS)
        text.postDelayed({
            if (isActive()) {
                text.setText(R.string.preparation_go)
                text.setTextColor(ContextCompat.getColor(context, R.color.gold))
                text.setTextSize(TypedValue.COMPLEX_UNIT_SP, GO_TEXT_SIZE_SP)
            } else {
                holder.removeView(text)
            }
        }, READY_DURATION_MS * 2)
        text.postDelayed({
            if (isActive()) {
                holder.removeView(text)
                onFinished()
            } else {
                holder.removeView(text)
            }
        }, READY_DURATION_MS * 3)
    }

    private companion object {
        const val READY_DURATION_MS = 1_000L
        const val READY_TEXT_SIZE_SP = 40f
        const val GO_TEXT_SIZE_SP = 64f
    }
}
