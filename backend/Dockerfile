FROM node:lts-alpine

WORKDIR /app

COPY --chown=1000:1000 package*.json prisma.config.ts /app/
COPY --chown=1000:1000 src/ /app/src/
COPY --chown=1000:1000 prisma/ /app/prisma/

RUN npm ci \
    && npx prisma generate \
    && chown -R 1000:1000 /app

USER 1000:1000

CMD ["npm", "run", "start:prod"]
