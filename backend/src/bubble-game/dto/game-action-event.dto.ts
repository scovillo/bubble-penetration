import { IsInt, IsNumber, Matches, Max, Min } from 'class-validator';

export class GameActionEventDto {
  @Matches(/^o_[0-9a-z]+_[0-9a-f]{8}$/)
  objectId!: string;

  @IsInt()
  @Min(0)
  @Max(20 * 60 * 1000)
  timestampMs!: number;

  @IsNumber({ allowInfinity: false, allowNaN: false, maxDecimalPlaces: 6 })
  @Min(0)
  @Max(1)
  x!: number;

  @IsNumber({ allowInfinity: false, allowNaN: false, maxDecimalPlaces: 6 })
  @Min(0)
  @Max(1)
  y!: number;
}
