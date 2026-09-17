package org.codeberg.scovillo.bubble.game

import kotlin.math.sqrt

fun normalize(vector: FloatArray): Float {
    if (vector.size !in 2..3) return 0.0f
    var len = 0.0f
    for (f in vector) {
        len += f * f
    }
    len = sqrt(len.toDouble()).toFloat()
    for (i in vector.indices) vector[i] /= len
    return len
}
