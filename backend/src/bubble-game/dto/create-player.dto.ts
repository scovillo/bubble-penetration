import { IsString, MaxLength, MinLength } from 'class-validator';

export class CreatePlayerDto {
  @IsString()
  @MinLength(2)
  @MaxLength(20)
  username!: string;
}
