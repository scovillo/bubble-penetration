import { IsString, MaxLength, MinLength } from 'class-validator';

export class CreatePlayerDto {
  @IsString({ message: 'Username must be text.' })
  @MinLength(1, { message: 'Username must contain at least 1 character.' })
  @MaxLength(12, { message: 'Username must contain at most 12 characters.' })
  username!: string;
}
