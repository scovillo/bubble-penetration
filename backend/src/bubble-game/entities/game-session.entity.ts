import { Entity, ManyToOne, PrimaryKey, Property } from '@mikro-orm/core';
import { randomUUID } from 'node:crypto';
import { Player } from './player.entity';

@Entity({ tableName: 'bubble_game_sessions' })
export class GameSession {
  @PrimaryKey({ type: 'uuid' })
  id: string = randomUUID();

  @ManyToOne(() => Player, { fieldName: 'player_id' })
  player!: Player;

  @Property({ length: 64 })
  seed!: string;

  @Property({ fieldName: 'started_at' })
  startedAt: Date = new Date();

  @Property({ fieldName: 'expires_at' })
  expiresAt!: Date;

  @Property({ fieldName: 'completed_at', nullable: true })
  completedAt?: Date;
}
