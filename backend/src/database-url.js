export function getDatabaseUrl() {
  const configuredUrl = process.env.DATABASE_URL;

  if (configuredUrl && !configuredUrl.includes("${")) {
    return configuredUrl;
  }

  const user = encodeURIComponent(process.env.POSTGRES_USER ?? "");
  const password = encodeURIComponent(process.env.POSTGRES_PASSWORD ?? "");
  const host = process.env.POSTGRES_HOST ?? "localhost";
  const port = process.env.POSTGRES_PORT ?? "5432";
  const database = process.env.POSTGRES_DB ?? "bubble_penetration";
  const schema = process.env.POSTGRES_SCHEMA ?? "public";

  return `postgresql://${user}:${password}@${host}:${port}/${database}?schema=${schema}`;
}
