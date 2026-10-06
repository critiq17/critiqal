FROM node:20-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

ARG VITE_API_URL=""
ENV VITE_API_URL=${VITE_API_URL}

RUN npm run build

FROM node:20-alpine AS runtime

WORKDIR /app

ENV NODE_ENV=production
ENV HOST=0.0.0.0
ENV PORT=3000

COPY package.json package-lock.json ./

RUN npm ci --omit=dev --ignore-scripts --no-audit --fund=false \
    && npm cache clean --force

COPY --from=build /app/build ./build

USER node 

EXPOSE 3000

CMD ["node", "build/index.js"]