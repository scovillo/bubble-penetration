FROM eclipse-temurin:17-jdk AS game-core-builder
WORKDIR /workspace

COPY gradlew build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle
COPY game-core ./game-core
# settings.gradle also includes the Android app, which is not needed to build game-core.
RUN mkdir app && ./gradlew :game-core:jsNodeProductionLibraryDistribution --no-daemon

FROM node:lts-bookworm-slim AS base
WORKDIR /app

FROM base AS builder
COPY --from=game-core-builder /workspace/game-core/build/dist/js/productionLibrary \
    /workspace/game-core/build/dist/js/productionLibrary
WORKDIR /workspace/backend
COPY backend/package.json backend/package-lock.json ./
RUN npm ci
COPY backend .
RUN npm test && ./node_modules/.bin/nest build

FROM base AS runtime
ENV NODE_ENV=production
# without this, the mikro-orm CLI forces preferTs=true and looks for migrations under src/ (not present here)
ENV MIKRO_ORM_CLI_USE_TS_NODE=false
# mikro-orm creates its configured migrations dir at boot; /app must be writable by the non-root user
RUN chown node:node /app
COPY --chown=node:node backend/package.json backend/package-lock.json backend/tsconfig.json ./

COPY --chown=node:node --from=game-core-builder \
    /workspace/game-core/build/dist/js/productionLibrary \
    /game-core/build/dist/js/productionLibrary
COPY --chown=node:node --from=builder /workspace/backend/node_modules ./node_modules
COPY --chown=node:node --from=builder /workspace/backend/dist ./dist

USER node
EXPOSE 3000
CMD ["npm", "run", "start:prod"]
