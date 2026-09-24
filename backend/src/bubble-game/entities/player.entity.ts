import { Entity, PrimaryKey, Property } from '@mikro-orm/core';
import { randomUUID } from 'node:crypto';

@Entity({ tableName: 'bubble_game_players' })
export class Player {
  @PrimaryKey({ type: 'uuid' })
  id: string = randomUUID();

  @Property({ fieldName: 'username', length: 20, unique: true })
  username!: string;

  @Property({ fieldName: 'username_key', length: 20, unique: true })
  usernameKey!: string;

  @Property({ fieldName: 'credential_hash', length: 64 })
  credentialHash!: string;

  @Property({ fieldName: 'created_at', onCreate: () => new Date() })
  createdAt: Date = new Date();

  @Property({ fieldName: 'blocked_at', nullable: true })
  blockedAt?: Date;

  @Property({ fieldName: 'last_active_at', nullable: true })
  lastActiveAt?: Date;
}
