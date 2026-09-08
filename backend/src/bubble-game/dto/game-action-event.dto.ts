import { IsIn, IsInt, Min } from 'class-validator';

export const GAME_ACTION_TYPES = [
  'bubble_match',
  'bubble_mismatch',
  'star',
] as const;

export type GameActionType = (typeof GAME_ACTION_TYPES)[number];

export class GameActionEventDto {
  @IsIn(GAME_ACTION_TYPES)
  type!: GameActionType;

  @IsInt()
  @Min(0)
  timestampMs!: number;
}
