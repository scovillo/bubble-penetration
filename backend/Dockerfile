FROM node:lts-alpine
WORKDIR /app
COPY .env package*.json prisma.config.ts /app/
COPY src/ /app/src/
COPY prisma/ /app/prisma/
RUN npm install
CMD ["npm", "run", "start:prod"]
