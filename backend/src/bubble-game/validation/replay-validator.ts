import { Logger } from '@nestjs/common';
import { createHmac } from 'node:crypto';
import { GameActionEventDto } from '../dto/game-action-event.dto';

export const REPLAY_VERSION = 1;

const INITIAL_TIMER_SECONDS = 25;
const SPAWN_INTERVAL_MS = 250;
const BASE_TRAVEL_DURATION_MS = 9_000;
const MIN_BUBBLE_SCALE = 0.75;
const BUBBLE_SCALE_RANGE = 0.15;
const STAR_SCALE = 0.85;
const MIN_EVENT_GAP_MS = 30;
const MIN_REACTION_MS = 100;
const COMBO_TIMEOUT_MS = 2_500;
const COMBO_COLLECT_FACTOR = 6;
const MAX_COMBO_MULTIPLIER = 16;
const TARGET_CHANGE_BASE_MS = 6_000;
const FINAL_TIME_TOLERANCE_MS = 500;
const HIT_TOLERANCE_FACTOR = 0.2;
const MAX_REPLAY_DURATION_MS = 20 * 60 * 1000;
const MAX_OBJECTS_ON_FIELD = 20;
const MIN_SPAWN_DISTANCE = 1.5;

export const REPLAY_COLORS = [
  'red',
  'green',
  'silver',
  'blue',
  'purple',
] as const;

export type ReplayColor = (typeof REPLAY_COLORS)[number];

export type SeededGameObject = {
  id: string;
  kind: 'bubble' | 'star';
  color?: ReplayColor;
  spawnAtMs: number;
  expiresAtMs: number;
  scale: number;
  radiusX: number;
  radiusY: number;
  startX: number;
  startY: number;
  endX: number;
  endY: number;
  speed: number;
};

export type ReplayResult = {
  score: number;
  finishedAtMs: number;
};

export class ReplayValidationError extends Error {}

export class DeterministicGenerator {
  constructor(private readonly seed: string) {
    if (!/^[0-9a-f]{64}$/.test(seed)) {
      throw new ReplayValidationError('invalid replay seed');
    }
  }

  objectAt(
    index: number,
    scoreAtSpawn: number,
    viewportAspectRatio = 1,
  ): SeededGameObject {
    if (!Number.isSafeInteger(index) || index < 0) {
      throw new ReplayValidationError('invalid object index');
    }

    const bytes = this.digest(`object:${index}`);
    const kind = bytes[0] < 13 ? 'star' : 'bubble';
    const color = kind === 'bubble' ? REPLAY_COLORS[bytes[1] % 5] : undefined;
    const bubbleScale =
      MIN_BUBBLE_SCALE + (bytes[2] / 255) * BUBBLE_SCALE_RANGE;
    const scale = kind === 'star' ? bubbleScale * STAR_SCALE : bubbleScale;
    const fieldHeight = viewportAspectRatio > 1 ? 10 : 10 / viewportAspectRatio;
    const fieldWidth = fieldHeight * viewportAspectRatio;
    const radiusX = scale / fieldWidth;
    const radiusY = scale / fieldHeight;
    const side = bytes[3] % 4;
    const startAlong = bytes.readUInt16BE(4) / 65_535;
    const endAlong = bytes.readUInt16BE(6) / 65_535;
    const randomSpeedFactor = bytes.readUInt16BE(8) / 65_535;
    const scaledSpeed = 1 + 6 * Math.log2(0.00125 * scoreAtSpawn + 1);
    const speedFactor = kind === 'star' ? 0.85 : 0.75;
    const randomFactor = kind === 'star' ? 0.15 : 0.25;
    const speed =
      scaledSpeed * (speedFactor + randomSpeedFactor * randomFactor);
    const [startX, startY, endX, endY] = this.pathFor(
      side,
      startAlong,
      endAlong,
      radiusX,
      radiusY,
    );
    const spawnAtMs = index * SPAWN_INTERVAL_MS;
    const expiresAtMs = spawnAtMs + Math.round(BASE_TRAVEL_DURATION_MS / speed);
    const tag = bytes.subarray(10, 14).toString('hex');

    return {
      id: `o_${index.toString(36)}_${tag}`,
      kind,
      color,
      spawnAtMs,
      expiresAtMs,
      scale,
      radiusX,
      radiusY,
      startX,
      startY,
      endX,
      endY,
      speed,
    };
  }

  nextTargetColor(changeIndex: number, current: ReplayColor): ReplayColor {
    const byte = this.digest(`target:${changeIndex}`)[0];
    const candidates = REPLAY_COLORS.filter((color) => color !== current);
    return candidates[byte % candidates.length];
  }

  objectForField(
    index: number,
    scoreAtSpawn: number,
    viewportAspectRatio: number,
    targetColor: ReplayColor,
    hasTargetBubble: boolean,
  ): SeededGameObject {
    const object = this.objectAt(index, scoreAtSpawn, viewportAspectRatio);
    return !hasTargetBubble
      ? { ...object, kind: 'bubble', color: targetColor }
      : object;
  }

  positionAt(
    object: SeededGameObject,
    timestampMs: number,
  ): {
    x: number;
    y: number;
  } {
    const progress =
      (timestampMs - object.spawnAtMs) /
      (object.expiresAtMs - object.spawnAtMs);

    return {
      x: object.startX + (object.endX - object.startX) * progress,
      y: object.startY + (object.endY - object.startY) * progress,
    };
  }

  private digest(domain: string): Buffer {
    return createHmac('sha256', Buffer.from(this.seed, 'hex'))
      .update(domain)
      .digest();
  }

  private pathFor(
    side: number,
    startAlong: number,
    endAlong: number,
    radiusX: number,
    radiusY: number,
  ): [number, number, number, number] {
    // Match the original renderer: an object starts and finishes half a radius
    // beyond the visible field instead of moving its centre along the border.
    const edgeOffsetX = radiusX / 2;
    const edgeOffsetY = radiusY / 2;
    switch (side) {
      case 0:
        return [startAlong, -edgeOffsetY, endAlong, 1 + edgeOffsetY];
      case 1:
        return [1 + edgeOffsetX, startAlong, -edgeOffsetX, endAlong];
      case 2:
        return [startAlong, 1 + edgeOffsetY, endAlong, -edgeOffsetY];
      default:
        return [-edgeOffsetX, startAlong, 1 + edgeOffsetX, endAlong];
    }
  }
}

export class ReplayValidator {
  private readonly logger = new Logger(ReplayValidator.name);
  private readonly generator: DeterministicGenerator;

  constructor(
    seed: string,
    private readonly match: {
      events: GameActionEventDto[];
      declaredScore: number;
      declaredDurationMs: number;
      viewportAspectRatio: number;
    },
    private readonly info: { playerId: string; sessionId: string },
  ) {
    this.generator = new DeterministicGenerator(seed);
  }

  validate(): ReplayResult {
    const { declaredDurationMs, declaredScore, events } = this.match;
    if (declaredDurationMs > MAX_REPLAY_DURATION_MS) {
      this.logger.warn(
        `Declared duration ${declaredDurationMs} exceeds maximum ${MAX_REPLAY_DURATION_MS} (playerId=${this.info.playerId}, sessionId=${this.info.sessionId})`,
      );
      throw new ReplayValidationError('replay duration exceeds maximum');
    }

    let score = 0;
    let timerSeconds = INITIAL_TIMER_SECONDS;
    let previousTimestampMs = 0;
    let comboCounter = 0;
    let lastMatchingBubbleAtMs: number | undefined;
    let targetColor: ReplayColor = 'red';
    let targetChangeIndex = 0;
    let nextTargetChangeAtMs = TARGET_CHANGE_BASE_MS;
    let nextObjectIndex = 0;
    const objects = new Map<string, SeededGameObject>();
    const hitObjectIds = new Set<string>();

    for (let eventIndex = 0; eventIndex < events.length; eventIndex += 1) {
      const event = events[eventIndex];

      if (event.timestampMs > declaredDurationMs) {
        this.logger.warn(
          `Event at timestamp ${event.timestampMs} exceeds declared duration ${declaredDurationMs} (playerId=${this.info.playerId}, sessionId=${this.info.sessionId})`,
        );
        throw new ReplayValidationError('event occurs after game duration');
      }
      if (
        eventIndex > 0 &&
        event.timestampMs - previousTimestampMs < MIN_EVENT_GAP_MS
      ) {
        this.logger.warn(
          `Event at timestamp ${event.timestampMs} occurs too close to previous event at ${previousTimestampMs} (playerId=${this.info.playerId}, sessionId=${this.info.sessionId})`,
        );
        throw new ReplayValidationError('events occur too close together');
      }

      timerSeconds -= (event.timestampMs - previousTimestampMs) / 1_000;
      if (timerSeconds <= 0) {
        this.logger.warn(
          `Event at timestamp ${event.timestampMs} occurs after game over at ${event.timestampMs + timerSeconds * 1_000} (playerId=${this.info.playerId}, sessionId=${this.info.sessionId})`,
        );
        throw new ReplayValidationError('hit occurs after game over');
      }

      while (
        Math.min(nextTargetChangeAtMs, nextObjectIndex * SPAWN_INTERVAL_MS) <=
        event.timestampMs
      ) {
        const spawnAtMs = nextObjectIndex * SPAWN_INTERVAL_MS;
        if (nextTargetChangeAtMs <= spawnAtMs) {
          targetColor = this.generator.nextTargetColor(
            targetChangeIndex,
            targetColor,
          );
          targetChangeIndex += 1;
          const scoreScale = Math.max(0.75, 1 - score / 400);
          nextTargetChangeAtMs += Math.round(
            TARGET_CHANGE_BASE_MS * scoreScale,
          );
          continue;
        }

        const activeObjectCount = [...objects.values()].filter(
          (candidate) =>
            candidate.spawnAtMs <= spawnAtMs &&
            candidate.expiresAtMs >= spawnAtMs &&
            !hitObjectIds.has(candidate.id),
        ).length;
        if (activeObjectCount >= MAX_OBJECTS_ON_FIELD) {
          nextObjectIndex += 1;
          continue;
        }

        const hasTargetBubble = [...objects.values()].some(
          (candidate) =>
            candidate.kind === 'bubble' &&
            candidate.color === targetColor &&
            candidate.expiresAtMs >= spawnAtMs &&
            !hitObjectIds.has(candidate.id),
        );
        const object = this.generator.objectForField(
          nextObjectIndex,
          score,
          this.match.viewportAspectRatio,
          targetColor,
          hasTargetBubble,
        );
        if (!this.hasMinimumSpawnDistance(object, objects, hitObjectIds)) {
          nextObjectIndex += 1;
          continue;
        }
        objects.set(object.id, object);
        nextObjectIndex += 1;
      }

      const object = objects.get(event.objectId);
      if (!object) {
        this.logger.warn(
          `Unknown object id ${event.objectId} at timestamp ${event.timestampMs} (${this.info.playerId}, ${this.info.sessionId})`,
        );
        throw new ReplayValidationError('unknown object id');
      }
      if (hitObjectIds.has(object.id)) {
        this.logger.warn(
          `Object id ${event.objectId} at timestamp ${event.timestampMs} was already hit (playerId=${this.info.playerId}, sessionId=${this.info.sessionId})`,
        );
        throw new ReplayValidationError('object was hit more than once');
      }
      if (event.timestampMs < object.spawnAtMs + MIN_REACTION_MS) {
        this.logger.warn(
          `Object id ${event.objectId} at timestamp ${event.timestampMs} was hit before reaction window (spawned at ${object.spawnAtMs}) (playerId=${this.info.playerId}, sessionId=${this.info.sessionId})`,
        );
        throw new ReplayValidationError(
          'object was hit before reaction window',
        );
      }
      if (event.timestampMs > object.expiresAtMs) {
        this.logger.warn(
          `Object id ${event.objectId} at timestamp ${event.timestampMs} was hit after leaving the field (expired at ${object.expiresAtMs}) (playerId=${this.info.playerId}, sessionId=${this.info.sessionId})`,
        );
        throw new ReplayValidationError(
          'object was hit after leaving the field',
        );
      }

      const position = this.generator.positionAt(object, event.timestampMs);
      const hitDistance = Math.hypot(
        (event.x - position.x) / object.radiusX,
        (event.y - position.y) / object.radiusY,
      );
      if (hitDistance > 1 + HIT_TOLERANCE_FACTOR) {
        this.logger.warn(
          `Object id ${event.objectId} at timestamp ${event.timestampMs} was hit at (${event.x}, ${event.y}) which is too far from actual position (${position.x}, ${position.y}) with radii (${object.radiusX}, ${object.radiusY}) (playerId=${this.info.playerId}, sessionId=${this.info.sessionId})`,
        );
        throw new ReplayValidationError('tap does not hit claimed object');
      }
      if (
        lastMatchingBubbleAtMs !== undefined &&
        event.timestampMs - lastMatchingBubbleAtMs > COMBO_TIMEOUT_MS
      ) {
        comboCounter = 0;
      }

      const multiplier = this.comboMultiplier(comboCounter);
      const bubbleTime = this.bubbleTimeFor(object.speed);

      if (object.kind === 'star') {
        score += 3 * multiplier;
        timerSeconds += bubbleTime * 2;
      } else if (object.color === targetColor) {
        score += multiplier;
        timerSeconds += bubbleTime;
        comboCounter += 1;
        lastMatchingBubbleAtMs = event.timestampMs;
      } else {
        timerSeconds -= bubbleTime * 1.5;
        comboCounter = 0;
        lastMatchingBubbleAtMs = undefined;
      }

      hitObjectIds.add(object.id);
      previousTimestampMs = event.timestampMs;
    }

    const finishedAtMs = Math.round(
      previousTimestampMs + Math.max(timerSeconds, 0) * 1_000,
    );

    if (Math.abs(declaredDurationMs - finishedAtMs) > FINAL_TIME_TOLERANCE_MS) {
      this.logger.warn(
        `Declared duration ${declaredDurationMs} does not match calculated finished time ${finishedAtMs} (playerId=${this.info.playerId}, sessionId=${this.info.sessionId})`,
      );
      throw new ReplayValidationError(
        'declared game duration is not plausible',
      );
    }
    if (score !== declaredScore) {
      this.logger.warn(
        `Declared score ${declaredScore} does not match calculated score ${score} (playerId=${this.info.playerId}, sessionId=${this.info.sessionId})`,
      );
      throw new ReplayValidationError('declared score does not match replay');
    }

    return { score, finishedAtMs };
  }

  private hasMinimumSpawnDistance(
    candidate: SeededGameObject,
    objects: Map<string, SeededGameObject>,
    hitObjectIds: Set<string>,
  ): boolean {
    const fieldHeight =
      this.match.viewportAspectRatio > 1
        ? 10
        : 10 / this.match.viewportAspectRatio;
    const fieldWidth = fieldHeight * this.match.viewportAspectRatio;
    const candidateX = candidate.startX * fieldWidth;
    const candidateY = candidate.startY * fieldHeight;

    for (const existing of objects.values()) {
      if (
        hitObjectIds.has(existing.id) ||
        existing.spawnAtMs > candidate.spawnAtMs ||
        existing.expiresAtMs < candidate.spawnAtMs
      ) {
        continue;
      }
      const position = this.generator.positionAt(existing, candidate.spawnAtMs);
      const minimumDistance =
        0.5 * candidate.scale + 0.5 * existing.scale + MIN_SPAWN_DISTANCE;
      if (
        Math.abs(candidateX - position.x * fieldWidth) < minimumDistance &&
        Math.abs(candidateY - position.y * fieldHeight) < minimumDistance
      ) {
        return false;
      }
    }
    return true;
  }

  private bubbleTimeFor(speed: number): number {
    return 1.25 - 0.25 * Math.log2(0.75 * speed + 1);
  }

  private comboMultiplier(counter: number): number {
    const factor = Math.floor(counter / COMBO_COLLECT_FACTOR);
    return factor > 0 ? Math.min(MAX_COMBO_MULTIPLIER, 2 ** factor) : 1;
  }
}
