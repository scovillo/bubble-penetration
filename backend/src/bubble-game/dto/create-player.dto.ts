import { IsString, MaxLength, MinLength } from 'class-validator';

export class CreatePlayerDto {
  @IsString()
  @MinLength(2)
  @MaxLength(12)
  username!: string;
}
