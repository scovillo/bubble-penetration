import { Migration } from '@mikro-orm/migrations';

export class Migration20260908190000 extends Migration {
  override up(): void {
    this.addSql(
      'alter table "bubble_game_sessions" add column "replay_version" integer not null default 1;',
    );
    this.addSql(`
      with duplicate_sessions as (
        select id,
          row_number() over (partition by player_id order by started_at desc, id desc) as row_number
        from bubble_game_sessions
        where completed_at is null
      )
      update bubble_game_sessions
      set completed_at = now()
      where id in (select id from duplicate_sessions where row_number > 1);
    `);
    this.addSql(
      'create unique index "bubble_game_sessions_one_open_per_player_idx" on "bubble_game_sessions" ("player_id") where "completed_at" is null;',
    );
  }

  override down(): void {
    this.addSql(
      'drop index if exists "bubble_game_sessions_one_open_per_player_idx";',
    );
    this.addSql(
      'alter table "bubble_game_sessions" drop column "replay_version";',
    );
  }
}
