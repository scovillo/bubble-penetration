package org.codeberg.scovillo.bubble.game.engine

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.HMAC
import dev.whyoleg.cryptography.algorithms.SHA256
import org.codeberg.scovillo.bubble.game.Boundaries
import org.codeberg.scovillo.bubble.game.GameObject
import org.codeberg.scovillo.bubble.game.GameObjectColor
import org.codeberg.scovillo.bubble.game.GameObjectType
import org.codeberg.scovillo.bubble.game.MatchState
import org.codeberg.scovillo.bubble.game.ReplayMetadata
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToLong

/** The production game model. It creates and updates only core [GameObject] values. */
class DeterministicMatchEngine(
    boundaries: Boundaries,
    config: MatchEngineConfig,
    state: MatchState,
) : MatchEngine(boundaries, config, state) {
    private val seed = hexToBytes(config.seed)
    private var nextObjectIndex = 0
    private var targetChangeIndex = 0
    private var nextTargetChangeAtMs = config.targetChangeBaseMs

    override suspend fun generateGameObjects(score: Int, elapsedMs: Long) {
        while (nextObjectIndex.toLong() * config.spawnIntervalMs <= elapsedMs && state.activeGameObjects.size < config.maxObjectsOnField) {
            val hasTarget = state.activeGameObjects.any {
                it.type == GameObjectType.BUBBLE && !it.isDisappearing && it.color == currentCollectColor
            }
            val generated = objectAt(nextObjectIndex, score, viewportAspectRatio)
            val spec = if (hasTarget) generated else generated.copy(
                type = GameObjectType.BUBBLE,
                color = currentCollectColor
            )
            if (elapsedMs - spec.spawnAtMs <= config.maxSpawnDelayMs && hasMinimumSpawnDistance(spec)) {
                state.activeGameObjects += createObject(spec)
            }
            nextObjectIndex++
        }
    }

    override suspend fun updateCollectColor(score: Int, elapsedMs: Long) {
        while (nextTargetChangeAtMs <= elapsedMs) {
            currentCollectColor =
                nextTargetColor(targetChangeIndex++, currentCollectColor)
            nextTargetChangeAtMs += (config.targetChangeBaseMs * maxOf(
                .75,
                1.0 - score / 400.0
            )).roundToLong()
        }
    }

    private fun createObject(spec: ReplayMetadata): GameObject {
        val metadata = ReplayMetadata(
            spec.objectId,
            spec.spawnAtMs,
            spec.expiresAtMs,
            spec.radiusX,
            spec.radiusY,
            spec.startX,
            spec.startY,
            spec.endX,
            spec.endY,
            if (spec.type == GameObjectType.BUBBLE) GameObjectType.BUBBLE else GameObjectType.STAR,
            spec.color,
            spec.scale,
            spec.speed
        )
        val type =
            if (spec.type == GameObjectType.BUBBLE) GameObjectType.BUBBLE else GameObjectType.STAR
        val value = GameObject(
            type,
            spec.color,
            if (type == GameObjectType.STAR) 3 else 1,
            spec.speed.toFloat(),
            spec.scale.toFloat(),
            replayMetadata = metadata
        )
        val width = boundaries.right - boundaries.left
        val height = boundaries.top - boundaries.bottom
        value.x = boundaries.left + spec.startX.toFloat() * width
        value.z = boundaries.top - spec.startY.toFloat() * height
        val seconds = (spec.expiresAtMs - spec.spawnAtMs) / 1_000f
        value.velocity = floatArrayOf(
            ((spec.endX - spec.startX) * width / seconds).toFloat() / value.speed,
            0f,
            (-(spec.endY - spec.startY) * height / seconds).toFloat() / value.speed
        )
        return value
    }

    private fun hasMinimumSpawnDistance(candidate: ReplayMetadata): Boolean {
        val width = boundaries.right - boundaries.left
        val height = boundaries.top - boundaries.bottom
        val candidateX = boundaries.left + candidate.startX.toFloat() * width
        val candidateZ = boundaries.top - candidate.startY.toFloat() * height
        return state.activeGameObjects.none { existing ->
            val metadata = existing.replayMetadata ?: return@none false
            if (metadata.spawnAtMs > candidate.spawnAtMs || metadata.expiresAtMs < candidate.spawnAtMs || existing.isDisappearing) return@none false
            val (x, y) = metadata.positionAt(candidate.spawnAtMs)
            val distance =
                .5f * candidate.scale.toFloat() + .5f * existing.scale + config.minSpawnDistance
            abs(candidateX - (boundaries.left + x.toFloat() * width)) < distance && abs(candidateZ - (boundaries.top - y.toFloat() * height)) < distance
        }
    }

    private suspend fun objectAt(index: Int, score: Int, aspect: Float): ReplayMetadata {
        val bytes = hmac("object:$index")
        val type = if (bytes.unsigned(0) < 13) GameObjectType.STAR else GameObjectType.BUBBLE
        val colors = GameObjectColor.entries.filter { it != GameObjectColor.GOLD }
        val color =
            if (type == GameObjectType.BUBBLE) colors[bytes.unsigned(1) % colors.size] else GameObjectColor.GOLD
        val bubbleScale =
            config.minBubbleScale + bytes.unsigned(2) / 255.0 * config.bubbleScaleRange
        val scale = if (type == GameObjectType.STAR) bubbleScale * config.starScale else bubbleScale
        val height = if (aspect > 1f) 10.0 else 10.0 / aspect
        val width = height * aspect
        val radiusX = scale / width
        val radiusY = scale / height
        val start = bytes.uint16(4) / 65535.0
        val end = bytes.uint16(6) / 65535.0
        val path = when (bytes.unsigned(3) % 4) {
            0 -> doubleArrayOf(start, -radiusY / 2, end, 1 + radiusY / 2)
            1 -> doubleArrayOf(1 + radiusX / 2, start, -radiusX / 2, end)
            2 -> doubleArrayOf(start, 1 + radiusY / 2, end, -radiusY / 2)
            else -> doubleArrayOf(-radiusX / 2, start, 1 + radiusX / 2, end)
        }
        val speedFactor = bytes.uint16(8) / 65535.0
        val baseSpeed = 1 + 4 * ln(.00125 * score + 1) / ln(2.0)
        val speed =
            baseSpeed * if (type == GameObjectType.STAR) .85 + speedFactor * .15 else .75 + speedFactor * .25
        val spawnAt = index.toLong() * config.spawnIntervalMs
        val expiresAt = spawnAt + (config.baseTravelDurationMs / speed).roundToLong()
        val tag = bytes.sliceArray(10..13)
            .joinToString("") { it.unsigned().toString(16).padStart(2, '0') }
        return ReplayMetadata(
            "o_${index.toString(36)}_$tag",
            spawnAt,
            expiresAt,
            radiusX,
            radiusY,
            path[0],
            path[1],
            path[2],
            path[3],
            type,
            color,
            scale,
            speed
        )
    }

    private suspend fun nextTargetColor(index: Int, current: GameObjectColor): GameObjectColor {
        val choices =
            GameObjectColor.bubbleColors().filter { it != current && it != GameObjectColor.GOLD }
        return choices[hmac("target:$index")[0].unsigned() % choices.size]
    }

    private suspend fun hmac(message: String): ByteArray {
        val decoder = CryptographyProvider.Default.get(HMAC).keyDecoder(SHA256)
        val hmacKey = decoder.decodeFromByteArray(HMAC.Key.Format.RAW, seed)
        return hmacKey.signatureGenerator().generateSignature(message.encodeToByteArray())
    }

    private fun hexToBytes(value: String): ByteArray {
        require(value.length == 64 && value.all { it.isDigit() || it in 'a'..'f' }) { "Invalid replay seed." }
        return ByteArray(value.length / 2) {
            value.substring(it * 2, it * 2 + 2).toInt(16).toByte()
        }
    }

    private fun ByteArray.unsigned(index: Int) = this[index].toInt() and 0xff
    private fun Byte.unsigned() = toInt() and 0xff
    private fun ByteArray.uint16(index: Int) = (unsigned(index) shl 8) or unsigned(index + 1)
}
