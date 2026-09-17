package org.codeberg.scovillo.bubble.game

interface Disappearance {

    val isDisappearing: Boolean
    val isDisappearFinished: Boolean
    fun disappear(): Boolean

}