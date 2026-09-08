import {
  Entity,
  Index,
  ManyToOne,
  OneToOne,
  PrimaryKey,
  Property,
} from '@mikro-orm/core';
import { randomUUID } from 'node:crypto';
import { GameSession } from './game-session.entity';
import { Player } from './player.entity';

@Entity({ tableName: 'bubble_game_highscores' })
@Index({
  name: 'bubble_game_highscores_leaderboard_idx',
  properties: ['score', 'confirmedAt'],
})
export class Highscore {
  @PrimaryKey({ type: 'uuid' })
  id: string = randomUUID();

  @ManyToOne(() => Player, { fieldName: 'player_id' })
  player!: Player;

  @OneToOne(() => GameSession, {
    owner: true,
    fieldName: 'session_id',
    unique: true,
  })
  session!: GameSession;

  @Property()
  score!: number;

  @Property({ fieldName: 'confirmed_at', onCreate: () => new Date() })
  confirmedAt: Date = new Date();
}
