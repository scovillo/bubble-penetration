package org.codeberg.scovillo.bubble.game.engine

import org.codeberg.scovillo.bubble.game.GameObjectColor

data class MatchSnapshot(
    val score: Int,
    val timerSeconds: Float,
    val initialTimerSeconds: Float,
    val collectColor: GameObjectColor,
    val comboProgress: Int,
    val comboMultiplier: Int,
)

sealed interface MatchEvent {
    data class StateChanged(val snapshot: MatchSnapshot) : MatchEvent
    data object TimerAlarm : MatchEvent
    data class TargetCollected(
        val timerDeltaSeconds: Float,
        val scoreDelta: Int,
        val wasCorrectColor: Boolean,
        val isStar: Boolean,
    ) : MatchEvent

    data object GameOver : MatchEvent
}
