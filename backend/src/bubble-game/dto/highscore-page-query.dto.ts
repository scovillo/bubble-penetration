import { Type } from 'class-transformer';
import {
  IsInt,
  IsOptional,
  IsString,
  Max,
  MaxLength,
  Min,
} from 'class-validator';

export const MAX_LEADERBOARD_LIMIT = 100;

export class HighscorePageQueryDto {
  @IsOptional()
  @IsString()
  @MaxLength(12)
  username?: string;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  startRank?: number;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(MAX_LEADERBOARD_LIMIT)
  limit?: number;
}
