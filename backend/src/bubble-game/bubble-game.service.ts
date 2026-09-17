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
import { SubmitScoreDto } from './dto/submit-score.dto';
import { GameSession } from './entities/game-session.entity';
import { Highscore } from './entities/highscore.entity';
import { Player } from './entities/player.entity';
import {
  REPLAY_VERSION,
  validateReplay,
} from './validation/game-core-replay-validator';

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
  replayVersion: number;
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
const SUBMISSION_CLOCK_TOLERANCE_MS = 1_000;

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
    });

    if (activeSession) {
      activeSession.completedAt = now;
      await this.gameSessions.getEntityManager().flush();
    }

    const session = this.gameSessions.create({
      player,
      seed: randomBytes(32).toString('hex'),
      replayVersion: REPLAY_VERSION,
      startedAt: now,
      expiresAt: new Date(now.getTime() + SESSION_MAX_DURATION_MS),
    });

    await this.gameSessions.getEntityManager().persist(session).flush();

    this.logger.debug(`Created session ${session.id} for player ${player.id}.`);

    return {
      sessionId: session.id,
      seed: session.seed,
      replayVersion: session.replayVersion,
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
      this.logger.warn(
        `Rejected score submission for player ${player.id}, session ${sessionId}: ` +
          'session not found.',
      );
      throw new NotFoundException('Game session not found.');
    }

    if (session.player.id !== player.id) {
      this.logger.warn(
        `Rejected score submission for player ${player.id}, session ${session.id}: ` +
          'session belongs to another player.',
      );
      throw new ForbiddenException(
        'This game session belongs to another player.',
      );
    }

    if (session.completedAt) {
      this.logger.warn(
        `Rejected score submission for player ${player.id}, session ${session.id}: ` +
          'session has already been submitted.',
      );
      throw new ConflictException(
        'This game session has already been submitted.',
      );
    }

    const now = new Date();

    if (now > session.expiresAt) {
      this.logger.warn(
        `Rejected score submission for player ${player.id}, session ${session.id}: ` +
          'session has expired.',
      );
      throw new BadRequestException('This game session has expired.');
    }

    const elapsedMs = now.getTime() - session.startedAt.getTime();
    if (dto.durationMs > elapsedMs + SUBMISSION_CLOCK_TOLERANCE_MS) {
      this.logger.warn(
        `Rejected score submission for player ${player.id}, session ${session.id}: ` +
          'submitted duration exceeds elapsed time.',
      );
      throw new BadRequestException('The submitted replay is not plausible.');
    }
    if (session.replayVersion !== REPLAY_VERSION) {
      this.logger.warn(
        `Rejected score submission for player ${player.id}, session ${session.id}: ` +
          'unsupported replay version.',
      );
      throw new BadRequestException('Unsupported replay version.');
    }

    try {
      this.logger.log(
        `Validating replay for player ${player.id}, session ${session.id}.`,
      );
      await validateReplay({
        seed: session.seed,
        score: dto.score,
        durationMs: dto.durationMs,
        viewportAspectRatio: dto.viewportAspectRatio,
        events: dto.events,
      });
    } catch (error: unknown) {
      const reason =
        error instanceof Error
          ? error.message
          : 'unexpected replay validation failure';
      this.logger.warn(
        `Rejected score submission for player ${player.id}, session ${session.id}: ` +
          reason,
      );
      throw new BadRequestException('The submitted replay is not plausible.');
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
        `score ${dto.score}, isPersonalBest=${isPersonalBest}.`,
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
      rank: number;
      total_count: number;
    };

    if (query.username) {
      const [userRow] = await connection.execute<{ rank: number }[]>(
        `${rankedCte} select rank from ranked where username = ?`,
        [query.username],
      );
      if (userRow) {
        startRank = Math.max(1, userRow.rank - PRECEDING_RANKS);
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
      score: row.score.toString(),
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
}
