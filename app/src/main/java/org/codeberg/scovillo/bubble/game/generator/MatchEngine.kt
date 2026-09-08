package org.codeberg.scovillo.bubble.game.generator

import org.codeberg.scovillo.bubble.game.Boundaries
import org.codeberg.scovillo.bubble.game.Bubble
import org.codeberg.scovillo.bubble.game.BubbleColor
import org.codeberg.scovillo.bubble.game.ComboState
import org.codeberg.scovillo.bubble.game.DisappearAnimation
import org.codeberg.scovillo.bubble.game.GameActionEvent
import org.codeberg.scovillo.bubble.game.GameActionLog
import org.codeberg.scovillo.bubble.game.GameObject
import org.codeberg.scovillo.bubble.game.MatchState
import org.codeberg.scovillo.bubble.game.Star
import org.codeberg.scovillo.bubble.game.Time
import org.codeberg.scovillo.bubble.game.TimerAlarm
import org.codeberg.scovillo.bubble.game.isOutside
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt

abstract class MatchEngine(
    val boundaries: Boundaries,
    val config: MatchEngineConfig,
    protected val state: MatchState,
) {
    private val objectsToBeRemoved = ArrayList<GameObject>()
    private val queuedTargets = ArrayList<QueuedTarget>()
    private val actionLog = GameActionLog()
    private val combo = ComboState()
    private val time = Time()
    private val timerAlarm = TimerAlarm()
    private var elapsedMs = 0L
    private var lastReplayTouchMs: Long? = null
    var isGameOver = false
        private set

    var currentCollectColor: BubbleColor = BubbleColor.RED
        protected set
    val viewportAspectRatio: Float
        get() {
            val measuredAspectRatio =
                (boundaries.right - boundaries.left) / (boundaries.top - boundaries.bottom)
            return round(measuredAspectRatio * 1_000_000) / 1_000_000
        }

    fun roundCoordinate(value: Double): Double =
        round(value * 1_000_000) / 1_000_000

    val gameObjects: List<GameObject> get() = state.activeGameObjects
    val durationMs: Long get() = elapsedMs
    val score: Int get() = state.score
    val timerSeconds: Float get() = state.timerValueMs
    val log: List<GameActionEvent> get() = actionLog.snapshot()

    fun advance(fracSec: Float, emit: (MatchEvent) -> Unit) {
        if (isGameOver) return
        elapsedMs += (fracSec * 1_000).toLong()
        combo.expireAt(elapsedMs)
        if (timerAlarm.shouldTrigger(state.timerValueMs, elapsedMs)) emit(MatchEvent.TimerAlarm)
        state.timerValueMs = (state.timerValueMs - fracSec).coerceAtLeast(0f)
        updateCollectColor(state.score, elapsedMs)
        updateGameObjects(fracSec, elapsedMs)
        resolveQueuedTargets(emit)
        if (state.timerValueMs <= 0f) {
            isGameOver = true
            emit(MatchEvent.GameOver)
        }
        emit(MatchEvent.StateChanged(snapshot()))
    }

    /** Advances a non-interactive scene, such as the animated main-menu backdrop. */
    fun advanceVisuals(fracSec: Float) {
        elapsedMs += (fracSec * 1_000).toLong()
        updateCollectColor(state.score, elapsedMs)
        updateGameObjects(fracSec, elapsedMs)
    }

    fun submitTap(
        normalizedX: Double,
        normalizedY: Double,
        worldX: Float,
        worldZ: Float,
    ): Boolean {
        if (isGameOver) return false
        synchronized(state.activeGameObjects) {
            val target = state.activeGameObjects
                .filterNot { it is DisappearAnimation && it.isDisappearFinished }
                .filter { isTapAllowed(it) }
                .minByOrNull { hitDistance(it, normalizedX, normalizedY, worldX, worldZ) }
                ?.takeIf {
                    hitDistance(it, normalizedX, normalizedY, worldX, worldZ) <=
                            if (it.replayMetadata == null) it.scale.toDouble() else 1.0
                }
                ?: return false
            target.replayMetadata?.let { lastReplayTouchMs = elapsedMs }
            queuedTargets.add(QueuedTarget(target, normalizedX, normalizedY))
            return true
        }
    }

    fun updateGameObjects(
        fracSec: Float,
        elapsedMs: Long,
    ) {
        synchronized(state.activeGameObjects) {
            state.activeGameObjects.forEach {
                it.update(fracSec)
                val replayExpired = it.replayMetadata?.let { metadata ->
                    elapsedMs > metadata.expiresAtMs
                } ?: false
                if (it is DisappearAnimation && it.isDisappearFinished || replayExpired || it.isOutside(
                        boundaries
                    )
                ) {
                    objectsToBeRemoved.add(it)
                }
            }
            for (gameObject in objectsToBeRemoved) {
                state.activeGameObjects.remove(gameObject)
            }
            objectsToBeRemoved.clear()
            generateGameObjects(state.score, elapsedMs)
        }
    }

    private fun isTapAllowed(gameObject: GameObject): Boolean {
        val metadata = gameObject.replayMetadata ?: return true
        if (elapsedMs < metadata.spawnAtMs + config.minReactionMs || elapsedMs > metadata.expiresAtMs) return false
        return lastReplayTouchMs?.let { elapsedMs - it >= config.minEventGapMs } != false
    }

    private fun hitDistance(
        gameObject: GameObject,
        x: Double,
        y: Double,
        worldX: Float,
        worldZ: Float
    ): Double {
        gameObject.replayMetadata?.let { metadata ->
            val (objectX, objectY) = metadata.positionAt(elapsedMs)
            return sqrt(
                ((x - objectX) / metadata.radiusX).pow(2) + ((y - objectY) / metadata.radiusY).pow(
                    2
                )
            )
        }
        return sqrt(
            (worldX - gameObject.x).toDouble().pow(2) + (worldZ - gameObject.z).toDouble().pow(2)
        )
    }

    private fun resolveQueuedTargets(emit: (MatchEvent) -> Unit) {
        synchronized(state.activeGameObjects) {
            queuedTargets.forEach { target ->
                resolveTarget(target.gameObject, target.x, target.y, emit)
            }
            queuedTargets.clear()
        }
    }

    private fun resolveTarget(
        gameObject: GameObject,
        x: Double,
        y: Double,
        emit: (MatchEvent) -> Unit
    ) {
        if (gameObject !is DisappearAnimation || !gameObject.disappear()) return
        val wasBubble = gameObject is Bubble
        val wasCorrect = wasBubble && gameObject.color == currentCollectColor
        val timerDelta = when (gameObject) {
            is Bubble -> if (wasCorrect) time.getBubbleTimeFor(gameObject.speed) else -time.getBubblePunishmentTimeFor(
                gameObject.speed
            )

            is Star -> time.getStarTimeFor(gameObject.speed)
            else -> return
        }
        val previousMultiplier = combo.multiplier
        val scoreDelta = if (wasCorrect || gameObject is Star) {
            when (gameObject) {
                is Bubble -> gameObject.score
                is Star -> gameObject.score
                else -> 0
            } * combo.multiplier
        } else 0
        state.timerValueMs += timerDelta
        state.score += scoreDelta
        if (wasCorrect) combo.increment(elapsedMs) else if (gameObject is Bubble) combo.reset()
        gameObject.replayMetadata?.let {
            actionLog.record(it.objectId, elapsedMs, x, y)
        }
        emit(
            MatchEvent.TargetCollected(
                timerDelta,
                scoreDelta,
                wasCorrect,
                gameObject is Star,
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
        combo.multiplier,
    )

    abstract fun generateGameObjects(score: Int, elapsedMs: Long)
    abstract fun updateCollectColor(score: Int, elapsedMs: Long)
}

private data class QueuedTarget(val gameObject: GameObject, val x: Double, val y: Double)
