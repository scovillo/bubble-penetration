import { Migration } from '@mikro-orm/migrations';

export class Migration20260916150000 extends Migration {
  override up(): void {
    this.addSql(
      'alter table "bubble_game_sessions" alter column "replay_version" set default 2;',
    );
  }

  override down(): void {
    this.addSql(
      'alter table "bubble_game_sessions" alter column "replay_version" set default 1;',
    );
  }
}
