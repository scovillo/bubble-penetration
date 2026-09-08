import { IsString, Matches, MaxLength, MinLength } from 'class-validator';

export class CreatePlayerDto {
  @IsString()
  @MinLength(3)
  @MaxLength(20)
  @Matches(/^[A-Za-z0-9](?:[A-Za-z0-9 _-]*[A-Za-z0-9])?$/)
  username!: string;
}
