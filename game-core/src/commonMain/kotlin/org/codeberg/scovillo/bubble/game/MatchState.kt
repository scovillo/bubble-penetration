package org.codeberg.scovillo.bubble.game

class MatchState(
    val activeGameObjects: ArrayList<GameObject> = ArrayList(),
    var timerValueMs: Float = 0f,
    var score: Int = 0,
) {
    val initialTimerMs = timerValueMs
}