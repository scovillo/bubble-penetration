import { Migration } from '@mikro-orm/migrations';

export class Migration20260908120000 extends Migration {
  override up(): void {
    this.addSql(`create table "bubble_game_players" (
      "id" uuid not null,
      "username" varchar(20) not null,
      "username_key" varchar(20) not null,
      "credential_hash" varchar(64) not null,
      "created_at" timestamptz not null,
      "blocked_at" timestamptz null,
      constraint "bubble_game_players_pkey" primary key ("id"),
      constraint "bubble_game_players_username_unique" unique ("username"),
      constraint "bubble_game_players_username_key_unique" unique ("username_key")
    );`);

    this.addSql(`create table "bubble_game_sessions" (
      "id" uuid not null,
      "player_id" uuid not null,
      "seed" varchar(64) not null,
      "started_at" timestamptz not null,
      "expires_at" timestamptz not null,
      "completed_at" timestamptz null,
      constraint "bubble_game_sessions_pkey" primary key ("id"),
      constraint "bubble_game_sessions_player_id_foreign" foreign key ("player_id") references "bubble_game_players" ("id") on update cascade
    );`);

    this.addSql(`create table "bubble_game_highscores" (
      "id" uuid not null,
      "player_id" uuid not null,
      "session_id" uuid not null,
      "score" integer not null check ("score" >= 0),
      "confirmed_at" timestamptz not null,
      constraint "bubble_game_highscores_pkey" primary key ("id"),
      constraint "bubble_game_highscores_session_id_unique" unique ("session_id"),
      constraint "bubble_game_highscores_player_id_foreign" foreign key ("player_id") references "bubble_game_players" ("id") on update cascade,
      constraint "bubble_game_highscores_session_id_foreign" foreign key ("session_id") references "bubble_game_sessions" ("id") on update cascade
    );`);

    this.addSql(
      'create index "bubble_game_highscores_leaderboard_idx" on "bubble_game_highscores" ("score" desc, "confirmed_at" asc);',
    );
    this.addSql(
      'create index "bubble_game_sessions_player_id_expires_at_idx" on "bubble_game_sessions" ("player_id", "expires_at");',
    );
  }

  override down(): void {
    this.addSql('drop table if exists "bubble_game_highscores" cascade;');
    this.addSql('drop table if exists "bubble_game_sessions" cascade;');
    this.addSql('drop table if exists "bubble_game_players" cascade;');
  }
}
