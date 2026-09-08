import { InjectRepository } from '@mikro-orm/nestjs';
import { EntityRepository } from '@mikro-orm/postgresql';
import {
  BadRequestException,
  ConflictException,
  ForbiddenException,
  Injectable,
  Logger,
  NotFoundException,
} from '@nestjs/common';
import { randomBytes } from 'node:crypto';
import { GameActionEventDto } from './dto/game-action-event.dto';
import { SubmitScoreDto } from './dto/submit-score.dto';
import { GameSession } from './entities/game-session.entity';
import { Highscore } from './entities/highscore.entity';
import { Player } from './entities/player.entity';

export type CreatedPlayer = {
  playerId: string;
  username: string;
  credential: string;
};

export type PlayerProfile = {
  playerId: string;
  username: string;
  createdAt: Date;
};

export type CreatedGameSession = {
  sessionId: string;
  seed: string;
  startedAt: Date;
  expiresAt: Date;
};

export type SubmittedHighscore = {
  highscoreId: string;
  score: number;
  isPersonalBest: boolean;
};

export type LeaderboardEntry = {
  rank: number;
  username: string;
  score: string;
  confirmedAt: Date;
};

export type HighscorePage = {
  highscores: LeaderboardEntry[];
  hasPrevious: boolean;
  hasNext: boolean;
};

const SESSION_MAX_DURATION_MS = 20 * 60 * 1000;

const BUBBLE_SCORE = 1;
const STAR_SCORE = 3;
const COMBO_COLLECT_FACTOR = 6;
const COMBO_TIMEOUT_MS = 2500;
const MAX_COMBO_MULTIPLIER = 16;

const MIN_EVENT_GAP_MS = 30;

const DEFAULT_LEADERBOARD_LIMIT = 20;
const MAX_LEADERBOARD_LIMIT = 100;
const PRECEDING_RANKS = 20;

@Injectable()
export class BubbleGameService {
  private readonly logger = new Logger(BubbleGameService.name);

  constructor(
    @InjectRepository(GameSession)
    private readonly gameSessions: EntityRepository<GameSession>,
    @InjectRepository(Highscore)
    private readonly highscores: EntityRepository<Highscore>,
  ) {}

  async createSession(player: Player): Promise<CreatedGameSession> {
    const now = new Date();
    const activeSession = await this.gameSessions.findOne({
      player,
      completedAt: null,
      expiresAt: { $gt: now },
    });

    if (activeSession) {
      activeSession.completedAt = now;
    }

    const session = this.gameSessions.create({
      player,
      seed: randomBytes(32).toString('hex'),
      startedAt: now,
      expiresAt: new Date(now.getTime() + SESSION_MAX_DURATION_MS),
    });

    await this.gameSessions.getEntityManager().persist(session).flush();

    this.logger.debug(`Created session ${session.id} for player ${player.id}.`);

    return {
      sessionId: session.id,
      seed: session.seed,
      startedAt: session.startedAt,
      expiresAt: session.expiresAt,
    };
  }

  async submitScore(
    player: Player,
    sessionId: string,
    dto: SubmitScoreDto,
  ): Promise<SubmittedHighscore> {
    const session = await this.gameSessions.findOne(
      { id: sessionId },
      { populate: ['player'] },
    );

    if (!session) {
      throw new NotFoundException('Game session not found.');
    }

    if (session.player.id !== player.id) {
      throw new ForbiddenException(
        'This game session belongs to another player.',
      );
    }

    if (session.completedAt) {
      throw new ConflictException(
        'This game session has already been submitted.',
      );
    }

    const now = new Date();

    if (now > session.expiresAt) {
      throw new BadRequestException('This game session has expired.');
    }

    const elapsedMs = now.getTime() - session.startedAt.getTime();
    this.assertPlausibleEventLog(player, session, dto.events, elapsedMs);

    const recomputedScore = this.recomputeScore(dto.events);

    if (recomputedScore !== dto.score) {
      this.logger.warn(
        `Rejected score submission for player ${player.id}, session ${session.id}: ` +
          `declared score ${dto.score} does not match recomputed score ${recomputedScore}.`,
      );
      throw new BadRequestException(
        'Submitted score does not match the action log.',
      );
    }

    session.completedAt = now;

    const previousBest = await this.highscores.findOne(
      { player },
      { orderBy: { score: 'DESC' } },
    );
    const isPersonalBest = !previousBest || dto.score > previousBest.score;

    const highscore = this.highscores.create({
      player,
      session,
      score: dto.score,
      confirmedAt: now,
    });

    await this.highscores.getEntityManager().persist(highscore).flush();

    this.logger.log(
      `Accepted score submission for player ${player.id}, session ${session.id}: ` +
        `score ${dto.score}, personalBest=${isPersonalBest}.`,
    );

    return {
      highscoreId: highscore.id,
      score: highscore.score,
      isPersonalBest,
    };
  }

  async getHighscorePage(query: {
    username?: string;
    startRank?: number;
    limit?: number;
  }): Promise<HighscorePage> {
    const limit = Math.min(
      Math.max(query.limit ?? DEFAULT_LEADERBOARD_LIMIT, 1),
      MAX_LEADERBOARD_LIMIT,
    );
    let startRank = Math.max(query.startRank ?? 1, 1);
    const connection = this.highscores.getEntityManager().getConnection();

    const rankedCte = `
      with best_per_player as (
        select distinct on (h.player_id) h.player_id, h.score, h.confirmed_at
        from bubble_game_highscores h
        order by h.player_id, h.score desc, h.confirmed_at asc
      ),
      ranked as (
        select
          p.username,
          b.score,
          b.confirmed_at,
          rank() over (order by b.score desc, b.confirmed_at asc) as rank,
          count(*) over () as total_count
        from best_per_player b
        join bubble_game_players p on p.id = b.player_id
      )
    `;

    type Row = {
      username: string;
      score: number;
      confirmed_at: Date;
      rank: string;
      total_count: string;
    };

    // A username anchors the page around that player's rank rather than replacing it.
    if (query.username) {
      const [userRow] = await connection.execute<{ rank: string }[]>(
        `${rankedCte} select rank from ranked where username = ?`,
        [query.username],
      );
      if (userRow) {
        startRank = Math.max(1, Number(userRow.rank) - PRECEDING_RANKS);
      }
    }

    const offset = startRank - 1;
    const rows = await connection.execute<Row[]>(
      `${rankedCte} select * from ranked order by rank asc limit ? offset ?`,
      [limit, offset],
    );

    const totalCount = rows.length > 0 ? Number(rows[0].total_count) : 0;
    const highscores = rows.map((row) => ({
      rank: Number(row.rank),
      username: row.username,
      score: String(row.score),
      confirmedAt: new Date(row.confirmed_at),
    }));

    const firstRank = highscores[0]?.rank ?? startRank;
    const lastRank = highscores[highscores.length - 1]?.rank ?? offset;

    return {
      highscores,
      hasPrevious: firstRank > 1,
      hasNext: lastRank < totalCount,
    };
  }

  private assertPlausibleEventLog(
    player: Player,
    session: GameSession,
    events: GameActionEventDto[],
    elapsedMs: number,
  ): void {
    let previousTimestampMs: number | undefined;

    for (const event of events) {
      if (event.timestampMs > elapsedMs) {
        this.rejectImplausibleLog(
          player,
          session,
          'event timestamp is after submission time',
        );
      }

      if (previousTimestampMs !== undefined) {
        const gap = event.timestampMs - previousTimestampMs;

        if (gap < 0) {
          this.rejectImplausibleLog(
            player,
            session,
            'event timestamps are not monotonic',
          );
        }

        if (gap < MIN_EVENT_GAP_MS) {
          this.rejectImplausibleLog(
            player,
            session,
            'events occur faster than humanly possible',
          );
        }
      }

      previousTimestampMs = event.timestampMs;
    }
  }

  private rejectImplausibleLog(
    player: Player,
    session: GameSession,
    reason: string,
  ): never {
    this.logger.warn(
      `Rejected score submission for player ${player.id}, session ${session.id}: ${reason}.`,
    );
    throw new BadRequestException('The submitted action log is not plausible.');
  }

  private recomputeScore(events: GameActionEventDto[]): number {
    let score = 0;
    let counter = 0;
    let lastBubbleTimestampMs: number | undefined;

    for (const event of events) {
      if (
        lastBubbleTimestampMs !== undefined &&
        event.timestampMs - lastBubbleTimestampMs > COMBO_TIMEOUT_MS
      ) {
        counter = 0;
      }

      switch (event.type) {
        case 'bubble_mismatch':
          counter = 0;
          break;
        case 'bubble_match':
          score += BUBBLE_SCORE * this.comboMultiplier(counter);
          counter += 1;
          lastBubbleTimestampMs = event.timestampMs;
          break;
        case 'star':
          score += STAR_SCORE * this.comboMultiplier(counter);
          break;
      }
    }

    return score;
  }

  private comboMultiplier(counter: number): number {
    const factor = Math.floor(counter / COMBO_COLLECT_FACTOR);
    return factor > 0 ? Math.min(MAX_COMBO_MULTIPLIER, 2 ** factor) : 1;
  }
}
