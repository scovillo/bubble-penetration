package org.codeberg.scovillo.bubble.game.engine

import org.codeberg.scovillo.bubble.game.Boundaries
import org.codeberg.scovillo.bubble.game.ComboState
import org.codeberg.scovillo.bubble.game.GameActionLog
import org.codeberg.scovillo.bubble.game.GameObject
import org.codeberg.scovillo.bubble.game.GameObjectColor
import org.codeberg.scovillo.bubble.game.GameObjectType
import org.codeberg.scovillo.bubble.game.MatchState
import org.codeberg.scovillo.bubble.game.Time
import org.codeberg.scovillo.bubble.game.TimerAlarm
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt

/** Complete headless match simulation. Renderers only consume [gameObjects]. */
abstract class MatchEngine(
    val boundaries: Boundaries,
    val config: MatchEngineConfig,
    protected val state: MatchState
) {
    private val queuedTargets = mutableListOf<QueuedTarget>()
    private val actionLog = GameActionLog()
    private val combo = ComboState()
    private val time = Time()
    private val timerAlarm = TimerAlarm()
    private var elapsedMs = 0L
    private var lastReplayTouchMs: Long? = null
    var isGameOver = false
        private set
    var currentCollectColor = GameObjectColor.RED
        protected set

    val viewportAspectRatio: Float get() = round((boundaries.right - boundaries.left) / (boundaries.top - boundaries.bottom) * 1_000_000) / 1_000_000
    fun roundCoordinate(value: Double): Double = round(value * 1_000_000) / 1_000_000
    val gameObjects: List<GameObject> get() = state.activeGameObjects
    val durationMs get() = elapsedMs
    val score get() = state.score
    val timerSeconds get() = state.timerValueMs
    val log get() = actionLog.snapshot()

    suspend fun advance(seconds: Float, emit: (MatchEvent) -> Unit) {
        if (isGameOver) return
        elapsedMs += (seconds * 1_000).toLong()
        combo.expireAt(elapsedMs)
        if (timerAlarm.shouldTrigger(state.timerValueMs, elapsedMs)) emit(MatchEvent.TimerAlarm)
        state.timerValueMs = (state.timerValueMs - seconds).coerceAtLeast(0f)
        updateCollectColor(state.score, elapsedMs)
        updateGameObjects(seconds, elapsedMs)
        queuedTargets.toList().forEach { resolveTarget(it, emit) }
        queuedTargets.clear()
        if (state.timerValueMs <= 0f) {
            isGameOver = true; emit(MatchEvent.GameOver)
        }
        emit(MatchEvent.StateChanged(snapshot()))
    }

    suspend fun advanceVisuals(seconds: Float) {
        elapsedMs += (seconds * 1_000).toLong(); updateCollectColor(
            state.score,
            elapsedMs
        ); updateGameObjects(seconds, elapsedMs)
    }

    /** Input is normalized screen space; OpenGL/world coordinates never enter the core. */
    fun submitTap(normalizedX: Double, normalizedY: Double): Boolean {
        if (isGameOver) return false
        val tappedAtMs = elapsedMs
        val target =
            state.activeGameObjects.filterNot { it.isDisappearFinished || it.isDisappearing }
                .filter(::isTapAllowed)
                .minByOrNull { hitDistance(it, normalizedX, normalizedY) }
                ?.takeIf { hitDistance(it, normalizedX, normalizedY) <= 1.0 } ?: return false
        target.replayMetadata?.let { lastReplayTouchMs = tappedAtMs }
        target.replayMetadata?.let { metadata ->
            actionLog.record(
                metadata.objectId,
                tappedAtMs,
                roundCoordinate(normalizedX),
                roundCoordinate(normalizedY),
            )
        }
        queuedTargets += QueuedTarget(target, normalizedX, normalizedY, tappedAtMs)
        return true
    }

    private suspend fun updateGameObjects(seconds: Float, nowMs: Long) {
        state.activeGameObjects.forEach { it.advance(seconds) }
        state.activeGameObjects.removeAll {
            it.isDisappearFinished || it.replayMetadata?.let { meta -> nowMs > meta.expiresAtMs } == true || isOutside(
                it
            )
        }
        generateGameObjects(state.score, nowMs)
    }

    private fun isOutside(value: GameObject): Boolean {
        val radius = value.scale / 2f
        return value.x < boundaries.left - radius ||
                value.x > boundaries.right + radius ||
                value.z < boundaries.bottom - radius ||
                value.z > boundaries.top + radius
    }

    private fun isTapAllowed(value: GameObject): Boolean {
        val meta = value.replayMetadata ?: return false
        return elapsedMs >= meta.spawnAtMs + config.minReactionMs && elapsedMs <= meta.expiresAtMs && (lastReplayTouchMs?.let { elapsedMs - it >= config.minEventGapMs } != false)
    }

    private fun hitDistance(value: GameObject, x: Double, y: Double): Double {
        val meta = value.replayMetadata ?: return Double.POSITIVE_INFINITY
        val (objectX, objectY) = meta.positionAt(elapsedMs)
        return sqrt(((x - objectX) / meta.radiusX).pow(2) + ((y - objectY) / meta.radiusY).pow(2))
    }

    private fun resolveTarget(queued: QueuedTarget, emit: (MatchEvent) -> Unit) {
        val value = queued.value
        if (!value.disappear()) return
        val correct = value.type == GameObjectType.BUBBLE && value.color == currentCollectColor
        val timerDelta = when (value.type) {
            GameObjectType.BUBBLE -> if (correct) time.getBubbleTimeFor(value.speed) else -time.getBubblePunishmentTimeFor(
                value.speed
            ); GameObjectType.STAR -> time.getStarTimeFor(value.speed)
        }
        val previousMultiplier = combo.multiplier
        val scoreDelta =
            if (correct || value.type == GameObjectType.STAR) value.score * combo.multiplier else 0
        state.timerValueMs += timerDelta; state.score += scoreDelta
        if (correct) combo.increment(elapsedMs) else if (value.type == GameObjectType.BUBBLE) combo.reset()
        emit(
            MatchEvent.TargetCollected(
                timerDelta,
                scoreDelta,
                correct,
                value.type == GameObjectType.STAR,
                combo.multiplier > previousMultiplier,
                combo.isActive
            )
        )
    }

    private fun snapshot() = MatchSnapshot(
        state.score,
        state.timerValueMs,
        state.initialTimerMs,
        currentCollectColor,
        combo.progress,
        combo.multiplier
    )

    abstract suspend fun generateGameObjects(score: Int, elapsedMs: Long)

    abstract suspend fun updateCollectColor(score: Int, elapsedMs: Long)
}

private data class QueuedTarget(
    val value: GameObject,
    val x: Double,
    val y: Double,
    val tappedAtMs: Long,
)
