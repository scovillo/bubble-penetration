package de.spicysources.bubblepenetration.util

import kotlin.math.roundToInt

object Utilities {
    fun normalize(vector: FloatArray): Float {
        if (vector.size < 2 || vector.size > 3) return 0.0f // invalid vector -> abort
        var len = 0.0f
        for (f in vector) {
            len += f * f
        }
        len = Math.sqrt(len.toDouble()).toFloat()
        for (i in vector.indices) vector[i] /= len
        return len
    }
}

fun roundToFirstDigit(number: Float): Float {
    return (number * 10).roundToInt() / 10f
}