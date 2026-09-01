package org.codeberg.scovillo.bubble.game

interface DisappearAnimation {

    val isDisappearFinished: Boolean
    fun disappear(): Boolean

}