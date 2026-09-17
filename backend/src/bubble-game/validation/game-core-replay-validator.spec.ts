import { simulateMatch } from '@bubble/game-core';
import { validateReplay } from './game-core-replay-validator';

jest.mock('@bubble/game-core', () => ({
  replayVersion: () => 1,
  simulateMatch: jest.fn(),
}));

const simulateMatchMock = jest.mocked(simulateMatch);

describe('validateReplay', () => {
  const input = {
    seed: '00'.repeat(32),
    score: 7,
    durationMs: 25_000,
    viewportAspectRatio: 1,
    events: [{ objectId: 'o_0_deadbeef', timestampMs: 400, x: 0.5, y: 0.5 }],
  };

  beforeEach(() => jest.resetAllMocks());

  it('delegates authoritative simulation to game-core', async () => {
    simulateMatchMock.mockResolvedValue({ score: 7, finishedAtMs: 25_100 });

    await expect(validateReplay(input)).resolves.toBeUndefined();

    expect(simulateMatchMock).toHaveBeenCalledWith({
      seed: input.seed,
      events: input.events,
      viewportAspectRatio: input.viewportAspectRatio,
    });
  });

  it('rejects an event after the submitted game duration without simulating it', async () => {
    await expect(
      validateReplay({
        ...input,
        events: [{ ...input.events[0], timestampMs: input.durationMs + 1 }],
      }),
    ).rejects.toThrow('event occurs after declared game duration');

    expect(simulateMatchMock).not.toHaveBeenCalled();
  });

  it('rejects a declared score that differs from the core result', async () => {
    simulateMatchMock.mockResolvedValue({ score: 8, finishedAtMs: 25_000 });

    await expect(validateReplay(input)).rejects.toThrow(
      'declared score does not match deterministic match',
    );
  });
});
