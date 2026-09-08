package org.codeberg.scovillo.bubble.game.generator

import org.codeberg.scovillo.bubble.game.Bubble
import org.codeberg.scovillo.bubble.game.BubbleColor
import org.codeberg.scovillo.bubble.game.Boundaries
import org.codeberg.scovillo.bubble.game.DisappearAnimation
import org.codeberg.scovillo.bubble.game.GameObject
import org.codeberg.scovillo.bubble.game.MatchState
import org.codeberg.scovillo.bubble.game.ReplayObjectMetadata
import org.codeberg.scovillo.bubble.game.Star
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToLong

/**
 * Replay object generation. Keep these constants and calculations byte-for-byte compatible
 * with the backend's DeterministicMatchEngine.
 */
class DeterministicMatchEngine(
    boundaries: Boundaries,
    config: MatchEngineConfig,
    state: MatchState,
) : MatchEngine(boundaries, config, state) {
    private val seedBytes = config.seed.hexToBytes()
    private var nextObjectIndex = 0
    private var targetChangeIndex = 0
    private var nextTargetChangeAtMs = config.targetChangeBaseMs

    override fun generateGameObjects(score: Int, elapsedMs: Long) {
        if (state.activeGameObjects.size >= config.maxObjectsOnField) {
            return
        }
        while (nextObjectIndex.toLong() * config.spawnIntervalMs <= elapsedMs && state.activeGameObjects.size < config.maxObjectsOnField) {
            val hasTargetBubble = state.activeGameObjects.any {
                it is Bubble && !it.isDisappearing && it.color == currentCollectColor
            }
            val spec = objectForField(
                nextObjectIndex,
                score,
                viewportAspectRatio,
                hasTargetBubble,
            )
            if (
                elapsedMs - spec.spawnAtMs <= MAX_SPAWN_DELAY_MS &&
                hasMinimumSpawnDistance(spec)
            ) {
                state.activeGameObjects.add(createGameObject(spec))
            }
            nextObjectIndex++
        }
    }

    override fun updateCollectColor(
        score: Int,
        elapsedMs: Long,
    ) {
        var color = currentCollectColor
        while (nextTargetChangeAtMs <= elapsedMs) {
            color = determineCollectColor(targetChangeIndex, color)
            targetChangeIndex++
            val scoreScale = maxOf(0.75, 1.0 - score / 400.0)
            nextTargetChangeAtMs += (config.targetChangeBaseMs * scoreScale).roundToLong()
        }
        currentCollectColor = color
    }

    internal fun objectAt(
        index: Int,
        scoreAtSpawn: Int,
        viewportAspectRatio: Float = 1.0f,
    ): ReplayObjectSpec {
        val bytes = digest("object:$index")
        val kind = if (bytes.unsigned(0) < 13) ObjectKind.STAR else ObjectKind.BUBBLE
        val color = if (kind == ObjectKind.BUBBLE) {
            BubbleColor.entries[bytes.unsigned(1) % BubbleColor.entries.size]
        } else {
            null
        }
        val bubbleScale =
            config.minBubbleScale + bytes.unsigned(2) / 255.0 * config.bubbleScaleRange
        val scale = if (kind == ObjectKind.STAR) bubbleScale * config.starScale else bubbleScale
        val fieldHeight = if (viewportAspectRatio > 1) 10.0 else 10.0 / viewportAspectRatio
        val fieldWidth = fieldHeight * viewportAspectRatio
        val radiusX = scale / fieldWidth
        val radiusY = scale / fieldHeight
        val side = bytes.unsigned(3) % 4
        val startAlong = bytes.uint16(4) / 65_535.0
        val endAlong = bytes.uint16(6) / 65_535.0
        val randomSpeedFactor = bytes.uint16(8) / 65_535.0
        val scaledSpeed = 1 + 6 * log2(0.00125 * scoreAtSpawn + 1)
        val speed = if (kind == ObjectKind.STAR) {
            scaledSpeed * (0.85 + randomSpeedFactor * 0.15)
        } else {
            scaledSpeed * (0.75 + randomSpeedFactor * 0.25)
        }
        val path = pathFor(side, startAlong, endAlong, radiusX, radiusY)
        val spawnAtMs = index.toLong() * config.spawnIntervalMs
        val expiresAtMs = spawnAtMs + (config.baseTravelDurationMs / speed).roundToLong()
        val tag =
            bytes.copyOfRange(10, 14).joinToString("") { "%02x".format(Locale.US, it.unsigned()) }

        return ReplayObjectSpec(
            objectId = "o_${index.toString(36)}_$tag",
            kind = kind,
            color = color,
            spawnAtMs = spawnAtMs,
            expiresAtMs = expiresAtMs,
            scale = scale,
            radiusX = radiusX,
            radiusY = radiusY,
            startX = path[0],
            startY = path[1],
            endX = path[2],
            endY = path[3],
            speed = speed,
        )
    }

    internal fun objectForField(
        index: Int,
        scoreAtSpawn: Int,
        viewportAspectRatio: Float,
        hasTargetBubble: Boolean,
    ): ReplayObjectSpec {
        val spec = objectAt(index, scoreAtSpawn, viewportAspectRatio)
        return if (hasTargetBubble) {
            spec
        } else {
            spec.copy(kind = ObjectKind.BUBBLE, color = currentCollectColor)
        }
    }

    private fun createGameObject(spec: ReplayObjectSpec): GameObject {
        val speed = spec.speed.toFloat()
        val gameObject = when (spec.kind) {
            ObjectKind.BUBBLE -> Bubble(spec.color, spec.color!!.rgb(), speed)
            ObjectKind.STAR -> Star(speed)
        }
        val metadata = ReplayObjectMetadata(
            objectId = spec.objectId,
            spawnAtMs = spec.spawnAtMs,
            expiresAtMs = spec.expiresAtMs,
            radiusX = spec.radiusX,
            radiusY = spec.radiusY,
            startX = spec.startX,
            startY = spec.startY,
            endX = spec.endX,
            endY = spec.endY,
        )
        gameObject.replayMetadata = metadata

        val (normalizedX, normalizedY) = metadata.positionAt(spec.spawnAtMs)
        val fieldWidth = boundaries.right - boundaries.left
        val fieldHeight = boundaries.top - boundaries.bottom
        val startX = boundaries.left + spec.startX.toFloat() * fieldWidth
        val startZ = boundaries.top - spec.startY.toFloat() * fieldHeight
        val endX = boundaries.left + spec.endX.toFloat() * fieldWidth
        val endZ = boundaries.top - spec.endY.toFloat() * fieldHeight
        val durationSeconds = (spec.expiresAtMs - spec.spawnAtMs) / 1_000f

        gameObject.scale = spec.scale.toFloat()
        gameObject.setPosition(
            boundaries.left + normalizedX.toFloat() * fieldWidth,
            0f,
            boundaries.top - normalizedY.toFloat() * fieldHeight,
        )
        gameObject.velocity = floatArrayOf(
            (endX - startX) / (durationSeconds * speed),
            0f,
            (endZ - startZ) / (durationSeconds * speed),
        )
        return gameObject
    }

    private fun hasMinimumSpawnDistance(candidate: ReplayObjectSpec): Boolean {
        val fieldWidth = boundaries.right - boundaries.left
        val fieldHeight = boundaries.top - boundaries.bottom
        val candidateX = boundaries.left + candidate.startX.toFloat() * fieldWidth
        val candidateZ = boundaries.top - candidate.startY.toFloat() * fieldHeight

        return state.activeGameObjects.none { existing ->
            val metadata = existing.replayMetadata ?: return@none false
            if (
                metadata.spawnAtMs > candidate.spawnAtMs ||
                metadata.expiresAtMs < candidate.spawnAtMs ||
                (existing is DisappearAnimation && existing.isDisappearing)
            ) {
                return@none false
            }
            val (normalizedX, normalizedY) = metadata.positionAt(candidate.spawnAtMs)
            val existingX = boundaries.left + normalizedX.toFloat() * fieldWidth
            val existingZ = boundaries.top - normalizedY.toFloat() * fieldHeight
            val minimumDistance = 0.5f * candidate.scale.toFloat() +
                    0.5f * existing.scale + MIN_SPAWN_DISTANCE
            abs(candidateX - existingX) < minimumDistance &&
                    abs(candidateZ - existingZ) < minimumDistance
        }
    }

    internal fun determineCollectColor(changeIndex: Int, current: BubbleColor): BubbleColor {
        val candidates = BubbleColor.entries.filter { it != current }
        return candidates[digest("target:$changeIndex").unsigned(0) % candidates.size]
    }

    private fun digest(domain: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(seedBytes, "HmacSHA256"))
        return mac.doFinal(domain.toByteArray(Charsets.UTF_8))
    }

    private fun pathFor(
        side: Int,
        startAlong: Double,
        endAlong: Double,
        radiusX: Double,
        radiusY: Double,
    ): DoubleArray {
        val edgeOffsetX = radiusX / 2
        val edgeOffsetY = radiusY / 2
        return when (side) {
            0 -> doubleArrayOf(startAlong, -edgeOffsetY, endAlong, 1 + edgeOffsetY)
            1 -> doubleArrayOf(1 + edgeOffsetX, startAlong, -edgeOffsetX, endAlong)
            2 -> doubleArrayOf(startAlong, 1 + edgeOffsetY, endAlong, -edgeOffsetY)
            else -> doubleArrayOf(-edgeOffsetX, startAlong, 1 + edgeOffsetX, endAlong)
        }
    }

    private fun String.hexToBytes(): ByteArray {
        require(matches(Regex("^[0-9a-f]{64}$"))) { "Invalid replay seed" }
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    private fun ByteArray.unsigned(index: Int): Int = this[index].toInt() and 0xff

    private fun ByteArray.uint16(index: Int): Int = (unsigned(index) shl 8) or unsigned(index + 1)

    private fun Byte.unsigned(): Int = toInt() and 0xff

    private fun log2(value: Double): Double = ln(value) / ln(2.0)

    private companion object {
        const val MAX_SPAWN_DELAY_MS = 100L
        const val MIN_SPAWN_DISTANCE = 1.5f
    }

    internal enum class ObjectKind { BUBBLE, STAR }

    internal data class ReplayObjectSpec(
        val objectId: String,
        val kind: ObjectKind,
        val color: BubbleColor?,
        val spawnAtMs: Long,
        val expiresAtMs: Long,
        val scale: Double,
        val radiusX: Double,
        val radiusY: Double,
        val startX: Double,
        val startY: Double,
        val endX: Double,
        val endY: Double,
        val speed: Double,
    )
}
