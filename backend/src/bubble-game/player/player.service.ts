import { InjectRepository } from '@mikro-orm/nestjs';
import { EntityRepository } from '@mikro-orm/postgresql';
import { ConflictException, Injectable, Logger } from '@nestjs/common';
import { createHash, randomBytes } from 'crypto';
import { Player } from '../entities/player.entity';

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

@Injectable()
export class PlayerService {
  private readonly logger = new Logger(PlayerService.name);

  constructor(
    @InjectRepository(Player)
    private readonly players: EntityRepository<Player>,
  ) {}

  async createPlayer(username: string): Promise<CreatedPlayer> {
    const usernameKey = username.toLowerCase();
    const existingPlayer = await this.players.findOne({ usernameKey });

    if (existingPlayer) {
      this.logger.warn(
        `Rejected player registration: username "${username}" is already in use.`,
      );
      throw new ConflictException('This username is already in use.');
    }

    const credential = randomBytes(32).toString('base64url');
    const player = this.players.create({
      username,
      usernameKey,
      credentialHash: createHash('sha256').update(credential).digest('hex'),
      createdAt: new Date(),
    });

    await this.players.getEntityManager().persist(player).flush();

    this.logger.log(`Registered player ${player.id} ("${username}").`);

    return {
      playerId: player.id,
      username: player.username,
      credential,
    };
  }

  getPlayerProfile(player: Player): PlayerProfile {
    return {
      playerId: player.id,
      username: player.username,
      createdAt: player.createdAt,
    };
  }

  async rotateCredential(player: Player): Promise<{ credential: string }> {
    const credential = randomBytes(32).toString('base64url');
    player.credentialHash = createHash('sha256')
      .update(credential)
      .digest('hex');

    await this.players.getEntityManager().flush();

    this.logger.log(`Rotated credential for player ${player.id}.`);

    return { credential };
  }
}
