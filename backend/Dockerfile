FROM node:lts-bookworm-slim AS base
WORKDIR /app

FROM base AS builder
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM base AS runtime
ENV NODE_ENV=production
# without this, the mikro-orm CLI forces preferTs=true and looks for migrations under src/ (not present here)
ENV MIKRO_ORM_CLI_USE_TS_NODE=false
# mikro-orm creates its configured migrations dir at boot; /app must be writable by the non-root user
RUN chown node:node /app
COPY --chown=node:node package.json package-lock.json tsconfig.json ./

COPY --chown=node:node --from=builder /app/node_modules ./node_modules
COPY --chown=node:node --from=builder /app/dist ./dist

USER node
EXPOSE 3000
CMD ["npm", "run", "start:prod"]
