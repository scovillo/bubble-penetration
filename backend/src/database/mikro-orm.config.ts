import { Migrator } from '@mikro-orm/migrations';
import { defineConfig } from '@mikro-orm/postgresql';
import 'dotenv/config';
import { GameSession } from '../bubble-game/entities/game-session.entity';
import { Highscore } from '../bubble-game/entities/highscore.entity';
import { Player } from '../bubble-game/entities/player.entity';

const databaseUrl = process.env.DATABASE_URL;

export default defineConfig({
  ...(databaseUrl
    ? { clientUrl: databaseUrl }
    : {
        host: process.env.POSTGRES_HOST ?? 'localhost',
        port: Number.parseInt(process.env.POSTGRES_PORT ?? '5432', 10),
        user: process.env.POSTGRES_USER ?? 'postgres',
        password: process.env.POSTGRES_PASSWORD ?? 'postgres',
        dbName: process.env.POSTGRES_DB ?? 'bubble_penetration',
      }),
  entities: [Player, GameSession, Highscore],
  extensions: [Migrator],
  // CLI auto-registers ts-node, which makes detectTsNode() true and would make the
  // Migrator look for migrations under pathTs (source), even when running compiled dist
  preferTs: false,
  migrations: {
    path: './dist/src/database/migrations',
    pathTs: './src/database/migrations',
  },
  connect: process.env.NODE_ENV !== 'test',
});
