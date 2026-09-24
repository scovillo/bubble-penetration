import { InjectRepository } from '@mikro-orm/nestjs';
import { EntityRepository } from '@mikro-orm/postgresql';
import { ConflictException, Injectable } from '@nestjs/common';
import { Player } from '../entities/player.entity';

/** Owns the case-insensitive uniqueness check used before player registration. */
@Injectable()
export class UsernameAvailabilityService {
  constructor(
    @InjectRepository(Player)
    private readonly players: EntityRepository<Player>,
  ) {}

  async ensureAvailable(username: string): Promise<void> {
    const existingPlayer = await this.players.findOne({
      usernameKey: username.toLowerCase(),
    });

    if (existingPlayer) {
      throw new ConflictException('This username is already in use.');
    }
  }
}
