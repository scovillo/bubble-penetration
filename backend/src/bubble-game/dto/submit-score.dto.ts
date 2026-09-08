import { Type } from 'class-transformer';
import {
  ArrayMaxSize,
  IsArray,
  IsInt,
  IsNumber,
  Max,
  Min,
  ValidateNested,
} from 'class-validator';
import { GameActionEventDto } from './game-action-event.dto';

export class SubmitScoreDto {
  @IsInt()
  @Min(0)
  score!: number;

  @IsInt()
  @Min(0)
  @Max(20 * 60 * 1000)
  durationMs!: number;

  @IsNumber({ allowInfinity: false, allowNaN: false, maxDecimalPlaces: 6 })
  @Min(0.25)
  @Max(4)
  viewportAspectRatio!: number;

  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => GameActionEventDto)
  @ArrayMaxSize(5000)
  events!: GameActionEventDto[];
}
