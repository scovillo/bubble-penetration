import { Migration } from '@mikro-orm/migrations';

export class Migration20260924210000 extends Migration {
  override up(): void {
    this.addSql(
      'alter table "bubble_game_players" add column "last_active_at" timestamptz null;',
    );
  }

  override down(): void {
    this.addSql(
      'alter table "bubble_game_players" drop column "last_active_at";',
    );
  }
}
