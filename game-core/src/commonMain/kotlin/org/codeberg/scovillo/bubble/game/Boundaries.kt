package org.codeberg.scovillo.bubble.game

/** The play field shared by the match rules and its renderer. */
class Boundaries {
    var top: Float = 0.0f
        private set
    var bottom: Float = 0.0f
        private set
    var left: Float = 0.0f
        private set
    var right: Float = 0.0f
        private set

    fun update(desiredHeight: Float, aspectRatio: Float) {
        top = desiredHeight / 2
        bottom = -desiredHeight / 2
        left = -(desiredHeight / 2 * aspectRatio)
        right = desiredHeight / 2 * aspectRatio
    }
}