import { InjectRepository } from '@mikro-orm/nestjs';
import { EntityRepository } from '@mikro-orm/postgresql';
import {
  CanActivate,
  ExecutionContext,
  Injectable,
  Logger,
  UnauthorizedException,
} from '@nestjs/common';
import { createHash } from 'node:crypto';
import { Player } from '../entities/player.entity';
import { PlayerRequest } from './player-request';

@Injectable()
export class PlayerCredentialGuard implements CanActivate {
  private readonly logger = new Logger(PlayerCredentialGuard.name);

  constructor(
    @InjectRepository(Player)
    private readonly players: EntityRepository<Player>,
  ) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest<PlayerRequest>();
    const credential = this.getBearerCredential(request.headers.authorization);

    if (!credential) {
      this.logger.warn(
        'Rejected request: missing/malformed bearer credential.',
      );
      throw new UnauthorizedException('A valid player credential is required.');
    }

    const credentialHash = createHash('sha256')
      .update(credential)
      .digest('hex');
    const player = await this.players.findOne({ credentialHash });

    if (!player) {
      this.logger.warn('Rejected request: unknown player credential.');
      throw new UnauthorizedException('A valid player credential is required.');
    }

    if (player.blockedAt) {
      this.logger.warn(`Rejected request: player ${player.id} is blocked.`);
      throw new UnauthorizedException('A valid player credential is required.');
    }

    request.player = player;
    return true;
  }

  private getBearerCredential(authorization?: string): string | undefined {
    const match = /^Bearer ([A-Za-z0-9_-]{43})$/.exec(authorization ?? '');
    return match?.[1];
  }
}
