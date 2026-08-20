import { PrismaPg } from "@prisma/adapter-pg";
import { PrismaClient } from "@prisma/client";
import "dotenv/config";
import pg from "pg";
import { getDatabaseUrl } from "./database-url.js";

const pool = new pg.Pool({
  connectionString: getDatabaseUrl(),
});

export const prisma = new PrismaClient({
  adapter: new PrismaPg(pool),
});
