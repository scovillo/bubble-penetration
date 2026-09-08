package org.codeberg.scovillo.bubble.game

interface DisappearAnimation {

    val isDisappearing: Boolean
    val isDisappearFinished: Boolean
    fun disappear(): Boolean

}