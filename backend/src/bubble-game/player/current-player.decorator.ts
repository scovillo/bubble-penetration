import { createParamDecorator, ExecutionContext } from '@nestjs/common';
import { Player } from '../entities/player.entity';
import { PlayerRequest } from './player-request';

export const CurrentPlayer = createParamDecorator(
  (_data: unknown, context: ExecutionContext): Player => {
    const request = context.switchToHttp().getRequest<PlayerRequest>();
    return request.player!;
  },
);
