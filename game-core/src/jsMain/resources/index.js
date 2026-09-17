const kotlinCore = require('./bubble-penetration-game-core.js');

const simulateCoreMatch =
  kotlinCore.org.codeberg.scovillo.bubble.game.engine.simulateMatch;

/**
 * Validates an authoritative deterministic replay.
 *
 * This is intentionally a normal JavaScript API: Kotlin/JS-generated namespaces and
 * representation details stay inside the package.
 */
exports.simulateMatch = async ({
  seed,
  events,
  viewportAspectRatio,
}) =>
  simulateCoreMatch(
    seed,
    events.map((event) => event.objectId),
    Float64Array.from(events.map((event) => event.timestampMs)),
    Float64Array.from(events.map((event) => event.x)),
    Float64Array.from(events.map((event) => event.y)),
    viewportAspectRatio,
  );
