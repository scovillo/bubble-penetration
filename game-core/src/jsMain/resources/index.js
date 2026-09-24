const kotlinCore = require('./bubble-penetration-game-core.js');

const simulateCoreMatch =
  kotlinCore.org.codeberg.scovillo.bubble.game.engine.simulateMatch;
const coreReplayVersion =
  kotlinCore.org.codeberg.scovillo.bubble.game.engine.replayVersion;
const coreEngineConfigSummary =
  kotlinCore.org.codeberg.scovillo.bubble.game.engine.engineConfigSummary;

exports.replayVersion = () => coreReplayVersion();
exports.engineConfigSummary = () => coreEngineConfigSummary();

/**
 * Validates an authoritative deterministic replay.
 *
 * This is intentionally a normal JavaScript API: Kotlin/JS-generated namespaces and
 * representation details stay inside the package.
 */
exports.simulateMatch = async ({
  seed,
  version,
  events,
  viewportAspectRatio,
}) =>
  simulateCoreMatch(
    seed,
    version,
    events.map((event) => event.objectId),
    Float64Array.from(events.map((event) => event.timestampMs)),
    Float64Array.from(events.map((event) => event.x)),
    Float64Array.from(events.map((event) => event.y)),
    viewportAspectRatio,
  );
