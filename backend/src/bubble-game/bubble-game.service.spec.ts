import { getRepositoryToken } from '@mikro-orm/nestjs';
import {
  BadRequestException,
  ConflictException,
  ForbiddenException,
  NotFoundException,
} from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import { BubbleGameService } from './bubble-game.service';
import { GameSession } from './entities/game-session.entity';
import { Highscore } from './entities/highscore.entity';
import { Player } from './entities/player.entity';

describe('BubbleGameService', () => {
  let service: BubbleGameService;
  let gameSessions: {
    findOne: jest.Mock;
    create: jest.Mock;
    getEntityManager: jest.Mock;
  };
  let highscores: {
    findOne: jest.Mock;
    create: jest.Mock;
    getEntityManager: jest.Mock;
  };

  const player = { id: 'player-1' } as Player;

  beforeEach(async () => {
    const em = {
      persist: jest.fn().mockReturnThis(),
      flush: jest.fn().mockResolvedValue(undefined),
      getConnection: jest.fn(),
    };
    gameSessions = {
      findOne: jest.fn(),
      create: jest.fn((data: object) => ({ id: 'session-1', ...data })),
      getEntityManager: jest.fn().mockReturnValue(em),
    };
    highscores = {
      findOne: jest.fn(),
      create: jest.fn((data: object) => ({ id: 'highscore-1', ...data })),
      getEntityManager: jest.fn().mockReturnValue(em),
    };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        BubbleGameService,
        { provide: getRepositoryToken(GameSession), useValue: gameSessions },
        { provide: getRepositoryToken(Highscore), useValue: highscores },
      ],
    }).compile();

    service = module.get<BubbleGameService>(BubbleGameService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  describe('createSession', () => {
    it('creates a new session when none is active', async () => {
      gameSessions.findOne.mockResolvedValue(null);

      const result = await service.createSession(player);

      expect(result.sessionId).toBe('session-1');
      expect(result.expiresAt.getTime()).toBeGreaterThan(
        result.startedAt.getTime(),
      );
    });

    it('discards a still-active session before creating a new one', async () => {
      const activeSession = { completedAt: undefined } as GameSession;
      gameSessions.findOne.mockResolvedValue(activeSession);

      await service.createSession(player);

      expect(activeSession.completedAt).toBeInstanceOf(Date);
    });
  });

  describe('submitScore', () => {
    function sessionFixture(overrides: Partial<GameSession> = {}): GameSession {
      const now = Date.now();
      return {
        id: 'session-1',
        player,
        seed: 'seed',
        startedAt: new Date(now - 10_000),
        expiresAt: new Date(now + 10_000),
        completedAt: undefined,
        ...overrides,
      };
    }

    it('rejects an unknown session', async () => {
      gameSessions.findOne.mockResolvedValue(null);

      await expect(
        service.submitScore(player, 'missing', { score: 0, events: [] }),
      ).rejects.toBeInstanceOf(NotFoundException);
    });

    it('rejects a session owned by another player', async () => {
      gameSessions.findOne.mockResolvedValue(
        sessionFixture({ player: { id: 'other-player' } as Player }),
      );

      await expect(
        service.submitScore(player, 'session-1', { score: 0, events: [] }),
      ).rejects.toBeInstanceOf(ForbiddenException);
    });

    it('rejects a session that was already submitted', async () => {
      gameSessions.findOne.mockResolvedValue(
        sessionFixture({ completedAt: new Date() }),
      );

      await expect(
        service.submitScore(player, 'session-1', { score: 0, events: [] }),
      ).rejects.toBeInstanceOf(ConflictException);
    });

    it('rejects an expired session', async () => {
      gameSessions.findOne.mockResolvedValue(
        sessionFixture({ expiresAt: new Date(Date.now() - 1_000) }),
      );

      await expect(
        service.submitScore(player, 'session-1', { score: 0, events: [] }),
      ).rejects.toBeInstanceOf(BadRequestException);
    });

    it('rejects events that occur faster than humanly possible', async () => {
      gameSessions.findOne.mockResolvedValue(sessionFixture());

      await expect(
        service.submitScore(player, 'session-1', {
          score: 2,
          events: [
            { type: 'bubble_match', timestampMs: 0 },
            { type: 'bubble_match', timestampMs: 5 },
          ],
        }),
      ).rejects.toBeInstanceOf(BadRequestException);
    });

    it('rejects a score that does not match the recomputed action log', async () => {
      gameSessions.findOne.mockResolvedValue(sessionFixture());

      await expect(
        service.submitScore(player, 'session-1', {
          score: 999,
          events: [{ type: 'bubble_match', timestampMs: 0 }],
        }),
      ).rejects.toBeInstanceOf(BadRequestException);
    });

    it('accepts a score matching the combo formula and records a highscore', async () => {
      gameSessions.findOne.mockResolvedValue(sessionFixture());
      highscores.findOne.mockResolvedValue(null);

      // 6 plain matches (multiplier stays 1x) followed by a 7th match at 2x (counter=6).
      const events = Array.from({ length: 7 }, (_, index) => ({
        type: 'bubble_match' as const,
        timestampMs: index * 40,
      }));

      const result = await service.submitScore(player, 'session-1', {
        score: 8,
        events,
      });

      expect(result.score).toBe(8);
      expect(result.isPersonalBest).toBe(true);
    });

    it('resets the combo on a mismatch and after a timeout', async () => {
      gameSessions.findOne.mockResolvedValue(sessionFixture());
      highscores.findOne.mockResolvedValue(null);

      const result = await service.submitScore(player, 'session-1', {
        score: 2,
        events: [
          { type: 'bubble_match', timestampMs: 0 },
          { type: 'bubble_mismatch', timestampMs: 100 },
          { type: 'bubble_match', timestampMs: 3_000 },
        ],
      });

      // match(1x=1) + mismatch(reset) + match after combo timeout(1x=1) = 2
      expect(result.score).toBe(2);
    });

    it('awards stars using the current multiplier without extending the combo', async () => {
      gameSessions.findOne.mockResolvedValue(sessionFixture());
      highscores.findOne.mockResolvedValue(null);

      const result = await service.submitScore(player, 'session-1', {
        score: 3,
        events: [{ type: 'star', timestampMs: 0 }],
      });

      expect(result.score).toBe(3);
    });

    it('reports isPersonalBest as false when a higher score already exists', async () => {
      gameSessions.findOne.mockResolvedValue(sessionFixture());
      highscores.findOne.mockResolvedValue({ score: 100 });

      const result = await service.submitScore(player, 'session-1', {
        score: 1,
        events: [{ type: 'bubble_match', timestampMs: 0 }],
      });

      expect(result.isPersonalBest).toBe(false);
    });
  });

  describe('getHighscorePage', () => {
    it('anchors the page around the rank of the given username', async () => {
      const execute = jest
        .fn()
        .mockResolvedValueOnce([{ rank: '25' }])
        .mockResolvedValueOnce([
          {
            username: 'alice',
            score: 42,
            confirmed_at: new Date(),
            rank: '25',
            total_count: '50',
          },
        ]);
      highscores.getEntityManager.mockReturnValue({
        getConnection: () => ({ execute }),
      });

      const result = await service.getHighscorePage({ username: 'alice' });

      expect(execute).toHaveBeenNthCalledWith(
        1,
        expect.stringContaining('where username = ?'),
        ['alice'],
      );
      expect(execute).toHaveBeenNthCalledWith(
        2,
        expect.stringContaining('order by rank asc limit ? offset ?'),
        [20, 4],
      );
      expect(result.highscores[0]).toMatchObject({
        rank: 25,
        username: 'alice',
        score: '42',
      });
      expect(result.hasPrevious).toBe(true);
      expect(result.hasNext).toBe(true);
    });

    it('falls back to the first page when the given username has no rank yet', async () => {
      const execute = jest
        .fn()
        .mockResolvedValueOnce([])
        .mockResolvedValueOnce([
          {
            username: 'champion',
            score: 99,
            confirmed_at: new Date(),
            rank: '1',
            total_count: '1',
          },
        ]);
      highscores.getEntityManager.mockReturnValue({
        getConnection: () => ({ execute }),
      });

      const result = await service.getHighscorePage({ username: 'newbie' });

      expect(execute).toHaveBeenNthCalledWith(
        2,
        expect.stringContaining('order by rank asc limit ? offset ?'),
        [20, 0],
      );
      expect(result.highscores[0]).toMatchObject({
        rank: 1,
        username: 'champion',
      });
    });

    it('paginates the leaderboard using limit/offset when no username is given', async () => {
      const execute = jest.fn().mockResolvedValue([
        {
          username: 'bob',
          score: 10,
          confirmed_at: new Date(),
          rank: '21',
          total_count: '25',
        },
      ]);
      highscores.getEntityManager.mockReturnValue({
        getConnection: () => ({ execute }),
      });

      const result = await service.getHighscorePage({
        startRank: 21,
        limit: 10,
      });

      expect(result.hasPrevious).toBe(true);
      expect(result.hasNext).toBe(true);
      expect(execute).toHaveBeenCalledWith(
        expect.stringContaining('order by rank asc limit ? offset ?'),
        [10, 20],
      );
    });

    it('reports no more pages when the last row reaches the total count', async () => {
      const execute = jest.fn().mockResolvedValue([]);
      highscores.getEntityManager.mockReturnValue({
        getConnection: () => ({ execute }),
      });

      const result = await service.getHighscorePage({});

      expect(result.hasPrevious).toBe(false);
      expect(result.hasNext).toBe(false);
    });
  });
});
