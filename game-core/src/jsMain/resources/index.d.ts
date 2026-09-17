export type ReplayEvent = {
  objectId: string;
  timestampMs: number;
  x: number;
  y: number;
};

export type MatchInput = {
  seed: string;
  events: ReplayEvent[];
  viewportAspectRatio: number;
};

export type ReplayResult = {
  score: number;
  finishedAtMs: number;
};

export function simulateMatch(input: MatchInput): Promise<ReplayResult>;
