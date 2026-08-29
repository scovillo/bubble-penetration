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
| TRUST_PROXY_HOPS  | Number of trusted reverse-proxy hops used to resolve the client IP. **Default is `0`.** |
| LOG_LEVEL         | Global log level. **\*\*Default is 'INFO'.\*\***                                |
| POSTGRES_HOST     | PostgreSQL hostname. **\*\*Default is '0.0.0.0'.\*\***                          |
| POSTGRES_PORT     | PostgreSQL port. **\*\*Default is '5432'.\*\***                                 |
| POSTGRES_USER     | PostgreSQL username.                                                            |
| POSTGRES_PASSWORD | Password of the PostgreSQL user.                                                |
| POSTGRES_DB       | PostgreSQL database to connect to. **\*\*Default is 'bubble_penetration'.\*\*** |
| POSTGRES_SCHEMA   | PostgreSQL database schema to connect to. **\*\*Default is 'public'.\*\***      |
| RATE_LIMIT_SERVICE_PER_MINUTE | Shared burst capacity for `/` and `/health` per client IP. **Default is `120`.** |
| RATE_LIMIT_READ_PER_MINUTE | Burst capacity for highscore reads per client IP. **Default is `120`.** |
| RATE_LIMIT_REGISTRATIONS_PER_MINUTE | Burst capacity for username registrations per client IP. **Default is `10`.** |
| RATE_LIMIT_SCORES_PER_MINUTE | Burst capacity for score uploads per client IP and username. **Default is `30`.** |

## Rate limiting

API limits use the maintained `express-rate-limit` middleware with separate,
generous quotas for reads, registrations, and score uploads. Responses expose
standard `RateLimit` headers. When a limit is reached, the API
returns HTTP `429` with a `Retry-After` header and a JSON error response. The root
endpoint and `/health` share a separate quota so health probes do not consume the
highscore API quota.

The built-in store is local to one backend process. Deployments with multiple
backend replicas should use a shared store or enforce equivalent limits at the
ingress so that limits apply consistently across replicas.

When the service runs behind a reverse proxy, set `TRUST_PROXY_HOPS` to the exact
number of trusted proxy hops (commonly `1`). Leaving it at `0` avoids trusting
spoofable forwarding headers when the backend is exposed directly.
