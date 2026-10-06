FROM node:20.19-alpine AS build

WORKDIR /app

# Пинним ту же major-версию pnpm, что использовалась в GitHub Actions
RUN npm install -g pnpm@9

COPY package.json pnpm-lock.yaml ./

RUN pnpm install --frozen-lockfile

COPY . .

RUN pnpm run build


FROM node:20.19-alpine AS runtime

WORKDIR /app

ENV NODE_ENV=production
ENV HOST=0.0.0.0
ENV PORT=3000

RUN npm install -g pnpm@9

COPY package.json pnpm-lock.yaml ./

RUN pnpm install \
    --prod \
    --frozen-lockfile \
    --node-linker=hoisted \
    --ignore-scripts

COPY --from=build /app/build ./build

USER node

EXPOSE 3000

CMD ["node", "build/index.js"]