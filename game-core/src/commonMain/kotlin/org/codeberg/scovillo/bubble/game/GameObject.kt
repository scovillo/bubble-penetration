package org.codeberg.scovillo.bubble.game

enum class GameObjectType {
    BUBBLE, STAR
}

/** Platform-neutral state used by match rules; renderers project it into their own transforms. */
class GameObject(
    val type: GameObjectType,
    val color: GameObjectColor,
    val score: Int,
    val speed: Float,
    var scale: Float = 1f,
    var velocity: FloatArray = FloatArray(3),
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f,
    var replayMetadata: ReplayMetadata? = null,
) {
    var isDisappearing: Boolean = false
        private set
    var disappearElapsedSeconds: Float = 0f
        private set
    var ageSeconds: Float = 0f
        private set

    val isDisappearFinished: Boolean
        get() = isDisappearing && disappearElapsedSeconds >= DISAPPEAR_DURATION_SECONDS

    fun advance(seconds: Float) {
        ageSeconds += seconds
        if (isDisappearing) {
            disappearElapsedSeconds += seconds
            return
        }
        x += seconds * velocity[0] * speed
        y += seconds * velocity[1] * speed
        z += seconds * velocity[2] * speed
    }

    fun disappear(): Boolean {
        if (isDisappearing) return false
        isDisappearing = true
        disappearElapsedSeconds = 0f
        return true
    }

    companion object {
        const val DISAPPEAR_DURATION_SECONDS = 0.22f
    }
}

data class ReplayMetadata(
    val objectId: String,
    val spawnAtMs: Long,
    val expiresAtMs: Long,
    val radiusX: Double,
    val radiusY: Double,
    val startX: Double,
    val startY: Double,
    val endX: Double,
    val endY: Double,
    val type: GameObjectType,
    val color: GameObjectColor,
    val scale: Double,
    val speed: Double,
) {
    fun positionAt(timestampMs: Long): Pair<Double, Double> {
        val progress = (timestampMs - spawnAtMs).toDouble() / (expiresAtMs - spawnAtMs)
        return startX + (endX - startX) * progress to startY + (endY - startY) * progress
    }
}
