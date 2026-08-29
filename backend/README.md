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

## Dev deployment

A push to `master` that changes the backend starts the Forgejo workflow
`.forgejo/workflows/backend-dev.yml`. It installs dependencies, runs the test
suite, generates the Prisma client, publishes `dev` and commit-specific images,
and rolls the commit-specific image out to the `bubble-dev` namespace.

The Forgejo repository must provide these action secrets:

| Secret              | Description                                                      |
| ------------------- | ---------------------------------------------------------------- |
| `REGISTRY_USERNAME` | Codeberg username                                                |
| `REGISTRY_PASSWORD` | Password or access token with permission to push images          |
| `KUBE_CONFIG`       | Base64-encoded kubeconfig with deployment access to `bubble-dev` |

Create the kubeconfig value without line wrapping, for example with
`base64 -w 0 ~/.kube/config`.

## Service information

`GET /` returns basic service metadata as JSON, including the backend version
and the available API versions. It also links to the health check (`/health`)
and the current API base path (`/api/v1`).

## Configuration

### Environment Variables

The following environment variables can be set to configure the application:

| Name                                | Description                                                                             |
| ----------------------------------- | --------------------------------------------------------------------------------------- |
| SERVER_PORT                         | Port on which the server is hosted. **\*\*Default is '3000'.\*\***                      |
| TRUST_PROXY_HOPS                    | Number of trusted reverse-proxy hops used to resolve the client IP. **Default is `0`.** |
| LOG_LEVEL                           | Global log level. **\*\*Default is 'INFO'.\*\***                                        |
| POSTGRES_HOST                       | PostgreSQL hostname. **\*\*Default is '0.0.0.0'.\*\***                                  |
| POSTGRES_PORT                       | PostgreSQL port. **\*\*Default is '5432'.\*\***                                         |
| POSTGRES_USER                       | PostgreSQL username.                                                                    |
| POSTGRES_PASSWORD                   | Password of the PostgreSQL user.                                                        |
| POSTGRES_DB                         | PostgreSQL database to connect to. **\*\*Default is 'bubble_penetration'.\*\***         |
| POSTGRES_SCHEMA                     | PostgreSQL database schema to connect to. **\*\*Default is 'public'.\*\***              |
| RATE_LIMIT_SERVICE_PER_MINUTE       | Shared burst capacity for `/` and `/health` per client IP. **Default is `120`.**        |
| RATE_LIMIT_READ_PER_MINUTE          | Burst capacity for highscore reads per client IP. **Default is `120`.**                 |
| RATE_LIMIT_REGISTRATIONS_PER_MINUTE | Burst capacity for username registrations per client IP. **Default is `10`.**           |
| RATE_LIMIT_SCORES_PER_MINUTE        | Burst capacity for score uploads per client IP and username. **Default is `30`.**       |

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

## Monthly highscore reset

The Kubernetes manifests install a `reset-highscores` CronJob. It runs at
midnight UTC on the first day of every month (`0 0 1 * *`) and sets every
positive score to zero. Usernames remain registered. The operation is
idempotent and overlapping jobs are forbidden. The `timeZone` field requires
Kubernetes 1.27 or newer.

To test the production job manually in MicroK8s:

```sh
microk8s kubectl create job \
	--from=cronjob/reset-highscores \
	reset-highscores-manual \
	--namespace=bubble-prod
microk8s kubectl logs \
	job/reset-highscores-manual \
	--namespace=bubble-prod
```
