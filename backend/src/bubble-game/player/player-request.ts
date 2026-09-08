import type { Request } from 'express';
import { Player } from '../entities/player.entity';

export type PlayerRequest = Request & {
  player?: Player;
};
