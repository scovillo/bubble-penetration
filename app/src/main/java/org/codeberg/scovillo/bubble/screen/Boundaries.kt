package org.codeberg.scovillo.bubble.screen

class Boundaries {

    var top: Float = 0.0f
        private set
    var bottom: Float = 0.0f
        private set
    var left: Float = 0.0f
        private set
    var right: Float = 0.0f
        private set

    fun updateWith(desired_height: Float, aspectRatio: Float) {
        top = desired_height / 2
        bottom = -desired_height / 2
        left = -(desired_height / 2 * aspectRatio)
        right = desired_height / 2 * aspectRatio
    }

}