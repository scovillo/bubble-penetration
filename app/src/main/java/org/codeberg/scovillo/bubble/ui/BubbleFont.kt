package org.codeberg.scovillo.bubble.ui

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

/** Applies the game's display typeface to text controls while leaving system UI untouched. */
object BubbleFont {
    private const val TEXT_SIZE_SCALE = 1.12f
    private var cachedTypeface: Typeface? = null

    fun applyTo(view: View, scaleNonButtonText: Boolean = true) {
        when (view) {
            is TextView -> {
                view.typeface = typeface(view.context)
                if (scaleNonButtonText || view is Button) {
                    view.setTextSize(TypedValue.COMPLEX_UNIT_PX, view.textSize * TEXT_SIZE_SCALE)
                }
            }
            is ViewGroup -> repeat(view.childCount) { applyTo(view.getChildAt(it), scaleNonButtonText) }
        }
    }

    fun typeface(context: Context): Typeface = cachedTypeface ?: synchronized(this) {
        cachedTypeface ?: Typeface.createFromAsset(context.applicationContext.assets, "fonts/DynaPuff.ttf")
            .also { cachedTypeface = it }
    }
}
