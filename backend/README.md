# Bubble Penetration Backend

The backend for the game Bubble Penetration, which manages the high scores.

## Running the backend

For a local development start, install the dependencies and start the service:

```sh
npm install
npm start
```

Configure the database connection through the environment variables listed
below before using the highscore API.

The latest container image can be downloaded from Codeberg:

```sh
docker pull codeberg.org/scovillo/bubble-penetration-backend:latest
```

## Service information

`GET /` returns basic service metadata as JSON, including the backend version
and the available API versions. It also links to the health check (`/health`)
and the current API base path (`/api/v1`).

## Configuration

### Environment Variables

The following environment variables can be set to configure the application:

| Name              | Description                                                                     |
| ----------------- | ------------------------------------------------------------------------------- |
| SERVER_PORT       | Port on which the server is hosted. **\*\*Default is '3000'.\*\***              |
| LOG_LEVEL         | Global log level. **\*\*Default is 'INFO'.\*\***                                |
| POSTGRES_HOST     | PostgreSQL hostname. **\*\*Default is '0.0.0.0'.\*\***                          |
| POSTGRES_PORT     | PostgreSQL port. **\*\*Default is '5432'.\*\***                                 |
| POSTGRES_USER     | PostgreSQL username.                                                            |
| POSTGRES_PASSWORD | Password of the PostgreSQL user.                                                |
| POSTGRES_DB       | PostgreSQL database to connect to. **\*\*Default is 'bubble_penetration'.\*\*** |
| POSTGRES_SCHEMA   | PostgreSQL database schema to connect to. **\*\*Default is 'public'.\*\***      |
