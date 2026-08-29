import assert from "node:assert/strict";
import { afterEach, test } from "node:test";
import { getDatabaseUrl } from "../src/database-url.js";

const databaseEnvironment = [
  "DATABASE_URL",
  "POSTGRES_USER",
  "POSTGRES_PASSWORD",
  "POSTGRES_HOST",
  "POSTGRES_PORT",
  "POSTGRES_DB",
  "POSTGRES_SCHEMA",
];

const originalEnvironment = Object.fromEntries(
  databaseEnvironment.map((name) => [name, process.env[name]]),
);

afterEach(() => {
  for (const [name, value] of Object.entries(originalEnvironment)) {
    if (value === undefined) {
      delete process.env[name];
    } else {
      process.env[name] = value;
    }
  }
});

test("uses an explicitly configured database URL", () => {
  process.env.DATABASE_URL = "postgresql://database.example/bubble";

  assert.equal(
    getDatabaseUrl(),
    "postgresql://database.example/bubble",
  );
});

test("builds and safely encodes the database URL from individual settings", () => {
  delete process.env.DATABASE_URL;
  process.env.POSTGRES_USER = "bubble user";
  process.env.POSTGRES_PASSWORD = "p@ss/word";
  process.env.POSTGRES_HOST = "postgres";
  process.env.POSTGRES_PORT = "5433";
  process.env.POSTGRES_DB = "highscores";
  process.env.POSTGRES_SCHEMA = "dev";

  assert.equal(
    getDatabaseUrl(),
    "postgresql://bubble%20user:p%40ss%2Fword@postgres:5433/highscores?schema=dev",
  );
});
