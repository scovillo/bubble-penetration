import { Type } from 'class-transformer';
import { ArrayMaxSize, IsInt, Min, ValidateNested } from 'class-validator';
import { GameActionEventDto } from './game-action-event.dto';

export class SubmitScoreDto {
  @IsInt()
  @Min(0)
  score!: number;

  @ValidateNested({ each: true })
  @Type(() => GameActionEventDto)
  @ArrayMaxSize(5000)
  events!: GameActionEventDto[];
}
