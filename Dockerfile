FROM node:12-alpine
COPY backend /bubble-backend
WORKDIR /bubble-backend
RUN npm install
CMD ["node", "app.js"]
