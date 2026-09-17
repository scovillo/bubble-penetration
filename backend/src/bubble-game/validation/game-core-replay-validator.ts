import {
  replayVersion,
  simulateMatch,
  type ReplayEvent,
} from '@bubble/game-core';

const FINISH_TIME_TOLERANCE_MS = 500;

/** The current replay protocol version, owned by game-core. */
export const REPLAY_VERSION = replayVersion();

export type ReplayValidationInput = {
  seed: string;
  score: number;
  durationMs: number;
  viewportAspectRatio: number;
  events: ReplayEvent[];
};

/**
 * Checks an untrusted score submission by replaying it with the exact shared game
 * rules. This deliberately has no game logic of its own: game-core remains the
 * only authority for object generation, hit testing, scoring, and game duration.
 */
export async function validateReplay({
  seed,
  score,
  durationMs,
  viewportAspectRatio,
  events,
}: ReplayValidationInput): Promise<void> {
  if (events.some((event) => event.timestampMs > durationMs)) {
    throw new Error('event occurs after declared game duration');
  }

  const result = await simulateMatch({
    seed,
    events,
    viewportAspectRatio,
  });

  if (result.score !== score) {
    throw new Error('declared score does not match deterministic match');
  }
  if (Math.abs(result.finishedAtMs - durationMs) > FINISH_TIME_TOLERANCE_MS) {
    throw new Error('declared duration does not match deterministic match');
  }
}
