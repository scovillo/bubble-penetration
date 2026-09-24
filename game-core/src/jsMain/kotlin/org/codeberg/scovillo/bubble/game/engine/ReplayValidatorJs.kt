package org.codeberg.scovillo.bubble.game.engine

@OptIn(ExperimentalJsExport::class)
@JsExport
class JsReplayResult(val score: Int, val finishedAtMs: Double)

/** Exposes the version from the canonical core configuration to API clients. */
@OptIn(ExperimentalJsExport::class)
@JsExport
fun replayVersion(): Int = MatchEngineConfig.CURRENT_REPLAY_VERSION

/** Exposes the seed-free engine configuration for startup diagnostics. */
@OptIn(ExperimentalJsExport::class)
@JsExport
fun engineConfigSummary(): String =
    MatchEngineConfig("", MatchEngineConfig.CURRENT_REPLAY_VERSION).summary()

/** Promise-based Node/browser facade for the common replay validator. */
@OptIn(ExperimentalJsExport::class)
@JsExport
suspend fun simulateMatch(
    seed: String,
    version: Int,
    objectIds: Array<String>,
    timestampsMs: DoubleArray,
    xs: DoubleArray,
    ys: DoubleArray,
    viewportAspectRatio: Double,
): JsReplayResult {
    require(objectIds.size == timestampsMs.size && xs.size == objectIds.size && ys.size == objectIds.size) {
        "replay event arrays have different lengths"
    }
    val events = objectIds.indices.map { index ->
        org.codeberg.scovillo.bubble.game.GameActionEvent(
            objectIds[index],
            timestampsMs[index].toLong(),
            xs[index],
            ys[index],
        )
    }
    val result = DeterministicMatch(seed, version)
        .replay(MatchInput(events, viewportAspectRatio))
    return JsReplayResult(result.score, result.finishedAtMs.toDouble())
}
