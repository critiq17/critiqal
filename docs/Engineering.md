# Critiqal — Engineering

## Repository Structure

```
critiqal/
  backend/
    core-service/           # Main API: users, feed, integrations, tokens
      src/main/java/
        org/critiqal/
          domain/           # Entities, value objects, domain events, repositories (interfaces)
          application/      # Use cases, application services, DTOs
          infrastructure/   # JPA repos, Redis, external clients, event publisher
          api/              # REST resources (Quarkus JAX-RS)
  event-service/            # Lightweight internal event bus (SSE push to clients)
    src/main/java/
      org/critiqal/event/
  frontend/
    src/
      lib/
        components/         # Reusable Svelte components
        stores/             # Svelte stores (auth, feed, user)
        services/           # API clients (typed, no fetch calls in components)
        types/              # Shared TypeScript types
      routes/               # Page components (SvelteKit file-based routing)
  infra/
    docker/
    nginx/
    .github/workflows/
  docs/
```

## Backend Architecture

### Domain Layer

Contains pure Java — no framework annotations, no database imports.

- **Entities**: `User`, `Post`, `ActivityEvent`, `TokenLedger`
- **Value Objects**: `UserId`, `Username`, `StravaActivity`, `GitHubCommit`, `TokenAmount`
- **Domain Events**: `ActivityPosted`, `TokensEarned`, `UserFollowed`
- **Repository interfaces**: defined here, implemented in infrastructure

### Application Layer

Orchestrates domain objects. One use case per class.

```
CreatePostUseCase
SyncStravaActivitiesUseCase
EarnTokensUseCase
FollowUserUseCase
```

### Infrastructure Layer

- **JPA repositories**: Quarkus + Hibernate Reactive
- **Redis**: session store, token cache, rate limiting
- **HTTP clients**: Strava API, GitHub API (Quarkus REST Client)
- **Event publisher**: writes domain events to the event-service via HTTP or in-process queue

### API Layer

RESTful JSON endpoints via Quarkus JAX-RS.

```
POST   /api/auth/login
POST   /api/auth/logout
GET    /api/users/{username}
GET    /api/feed
POST   /api/posts
GET    /api/integrations/github/connect
GET    /api/integrations/strava/connect
GET    /api/tokens/balance
```

### Authentication

- Cookie-based: `HttpOnly`, `Secure`, `SameSite=Strict`
- Session stored in Redis with TTL
- No JWT in cookies — session ID only

## Event Service

Standalone Quarkus service. Receives domain events from core-service and pushes them to connected clients via SSE.

- `POST /events/publish` — internal endpoint for core-service to push events
- `GET /events/stream` — SSE stream for authenticated frontend clients

Events: `feed.new_post`, `token.earned`, `notification.follow`, `integration.synced`

## Frontend Architecture

Svelte 5 with runes. SvelteKit for routing and SSR where applicable.

### State Management

- `authStore` — current user session
- `feedStore` — paginated post feed
- `notificationStore` — real-time SSE events
- `tokenStore` — current token balance

### Service Layer

All API calls go through typed service modules. Components never call `fetch` directly.

```
authService.ts
feedService.ts
integrationService.ts
tokenService.ts
```

### Component Guidelines

- One responsibility per component
- Props typed with TypeScript interfaces
- No business logic in components — delegate to stores and services
- Lazy-load heavy components with `{#await import(...)}`

## Database Schema (key tables)

```sql
users            (id, username, email, avatar_url, created_at)
sessions         (id, user_id, expires_at)             -- Redis mirror
posts            (id, user_id, content, source, source_ref, created_at)
follows          (follower_id, followee_id, created_at)
activity_events  (id, user_id, type, payload, synced_at)
token_ledger     (id, user_id, delta, reason, created_at)
integrations     (id, user_id, provider, access_token, refresh_token, expires_at)
```

## Infrastructure

### Docker Compose (local)

Services: `postgres`, `redis`, `core-service`, `event-service`, `frontend`, `nginx`

### Nginx

- Reverse proxy: `/api` -> core-service, `/events` -> event-service, `/` -> frontend
- TLS termination in production
- Gzip, caching headers for static assets

### CI/CD (GitHub Actions)

- `ci.yml` — build, test, lint on every PR
- `deploy.yml` — build Docker images and deploy on merge to `main`

## Coding Standards

- Java 21 features: records, sealed interfaces, pattern matching, text blocks
- No field injection (`@Inject` on fields is banned) — constructor injection only
- No mutable state in domain objects — return new instances
- Frontend: no `any` type, strict TypeScript, no inline styles
- Test coverage target: 80%+ (unit + integration)