package org.codeberg.scovillo.bubble.game

/** Stable color identifiers used by the game rules and replay protocol. */
enum class GameObjectColor {
    RED,
    GREEN,
    SILVER,
    BLUE,
    PURPLE,
    GOLD;

    companion object {
        fun bubbleColors() = listOf(RED, GREEN, SILVER, BLUE, PURPLE)
    }
}
