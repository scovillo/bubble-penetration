# AGENTS.md

Guidance for coding agents working in this repository.

## Repository overview

This is a monorepo containing three independent projects that don't share
tooling or dependencies:

- `app/` — Android game client, written in Kotlin, built with Gradle.
- `backend/` — NestJS/TypeScript API for global highscores, built with npm.
- `website/` + `scripts/build-website.mjs` — static landing page generator
  (Node.js, no framework), output goes to `_site/`.

Pick the section below matching the files you're changing. There is no
top-level build command that covers all three.

## Android app (`app/`)

- Language: Kotlin. Package root: `app/src/main/java/org/codeberg/scovillo/bubble`.
- Build tool: Gradle (wrapper at repo root: `./gradlew`).
- Two product flavors: `fdroid` (minSdk 14) and `play` (minSdk 21). Prefer
  `fdroid` variants for local builds/tests since they compile without
  Play-specific dependencies.
- Unit tests use JUnit 5 (`useJUnitPlatform()`), live under `app/src/test/java`.

Common commands (run from repo root):

```bash
./gradlew assembleFdroidDebug        # debug build
./gradlew testFdroidDebugUnitTest    # unit tests
./gradlew lint                       # Android lint
```

Notes:

- Release builds require signing env vars (`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`,
  `KEY_ALIAS`, `KEY_PASSWORD`) — not needed for debug/test work.
- The `generateReadme` Gradle task auto-generates `README.md` from
  `app/build.gradle` values and `fastlane/metadata/android/en-US/full_description.txt`.
  Don't hand-edit generated sections of `README.md`; update the source
  (build.gradle config or the fastlane description) instead.
- Async UI updates: never keep a strong casted view reference from a worker
  thread; resolve the view on the UI thread and null-check before updating.
- Whenever adding or changing user-facing Android text, update the string in
  every existing `app/src/main/res/values*/strings.xml` locale; do not rely on
  fallback-language resources for newly introduced UI text.

## Backend (`backend/`)

- NestJS app, TypeScript, MikroORM + PostgreSQL, Redis for throttling/cache.
- Run all commands from within `backend/`.

```bash
npm ci                # install
npm run start:dev     # dev server with watch
npm run build         # nest build
npm run lint          # eslint --fix over src/test
npm test              # jest unit tests (*.spec.ts colocated with source)
npm run test:e2e      # e2e tests in backend/test
npm run format        # prettier --write
```

Notes:

- Lint config is flat ESLint (`eslint.config.mjs`) with `typescript-eslint`
  recommendedTypeChecked + prettier. `no-explicit-any` is off;
  `no-floating-promises` and `no-unsafe-argument` are warnings, not errors.
- Migrations are managed with MikroORM CLI (`npm run migration:create` /
  `migration:up` / `migration:down`), config at `src/database/mikro-orm.config.ts`.
- CI (`.forgejo/workflows/backend-dev.yml`) runs `npm ci`, `npm test`,
  `npm run build` on every push touching `backend/**` — match this locally
  before considering backend work done.

### Security considerations

This API is public, open-source, and requires no signup — anyone can read
the source, register a player, and call every endpoint. Treat all input as
hostile and preserve the existing defenses:

- **No login, credential-only auth.** Players are identified by a random
  bearer token (see `player-credential.guard.ts`); only its SHA-256 hash is
  stored/compared. Never log the raw credential, never add an endpoint that
  returns `credentialHash`, and never relax the token format regex.
- **Rate limiting is the main abuse control.** Global `ThrottlerGuard`
  (Redis-backed in production, see `app.module.ts`) plus stricter per-route
  `@Throttle()` on mutating endpoints (e.g. player creation, credential
  rotation). Any new endpoint that writes data or triggers external calls
  needs an explicit, deliberately low throttle — don't rely on the global
  default alone.
- **Keep global validation strict.** `main.ts` uses
  `whitelist: true, forbidNonWhitelisted: true`. New DTOs must declare every
  accepted field explicitly with `class-validator` decorators; don't widen
  existing DTOs to accept free-form objects.
- **`blockedAt` must stay enforced.** `PlayerCredentialGuard` rejects blocked
  players; any new auth path (guard, decorator, service method) must check
  this too, not just credential validity.
- **No CORS/helmet configured today.** If you add CORS, scope it to known
  origins explicitly — don't wildcard `*` on an API that accepts
  credential-bearing requests. Consider security headers (helmet) when
  adding new public-facing routes.
- **`TRUST_PROXY_HOPS`/`REDIS_URL` are required in production** (enforced by
  `environment.validation.ts`) because rate limiting depends on the client
  IP being resolved correctly behind the reverse proxy — don't bypass this
  validation.

## Website (`website/`, `scripts/build-website.mjs`)

- Plain Node.js (ESM) script, no build framework or package.json of its own
  (uses the root project's Node runtime).
- Translations live in `website/translations.mjs`; templates are
  `website/template.html` and `website/privacy-template.html`.

```bash
node scripts/build-website.mjs   # generates static site into _site/
```

- The build script validates output: it throws if a template placeholder
  (`{{...}}`) is left unresolved or if the JSON-LD block is missing/invalid.
  Treat these as build failures, not warnings.

## General conventions

- Keep changes scoped to the project you're touching — don't mix Android,
  backend, and website edits in a way that conflates unrelated build systems.
- Prefer an object-oriented design: give distinct responsibilities their own
  focused classes and keep UI, persistence, and business logic separated.
- License is GPL-3.0; don't introduce dependencies with incompatible licenses.
- Prefer editing source-of-truth files over generated output (`README.md`
  Android section, `_site/` website output).
