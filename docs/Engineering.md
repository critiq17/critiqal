# Critiqal - Engineering

## Repository Structure

```text
critiqal/
  .github/workflows/         # CI and deployment pipelines
  backend/                   # Quarkus API
    pom.xml
    Dockerfile
    docker-compose*.yml
    src/main/java/org/acme/
      api/                   # REST controllers and transport DTOs
      application/strava/    # Strava orchestration use case
      domain/                # Feature modules: entity + repo + service
      infra/                 # External adapters and storage/persistence helpers
      utils/                 # JWT, password hashing, image processing
    src/main/resources/
      application.properties
      db/migration/          # Flyway SQL migrations
    src/test/java/           # Unit and integration tests
  frontend/                  # SvelteKit app
    package.json
    src/routes/              # File-based routes
    src/lib/
      actions/               # DOM actions
      api/                   # Low-level HTTP client and endpoint map
      components/            # Feature UI grouped by area
      features/              # Stateful feature hooks/classes
      motion/                # Animation helpers
      query/                 # Lightweight query/mutation primitives
      schemas/               # Zod schemas
      services/              # Typed API wrappers
      stores/                # App-wide state and caches
      tma/                   # Telegram Mini App helpers
      types/                 # Shared TypeScript types
      ui/                    # Reusable UI primitives
      utils/                 # Formatting and view helpers
    src/tests/e2e/           # Playwright tests
    static/assets/           # Static images and reaction assets
  docs/
```

Generated directories that should usually not be edited by hand:

- `backend/target`
- `frontend/build`
- `frontend/.svelte-kit`
- `frontend/coverage`
- `frontend/node_modules`

## Current Architecture

- The repository currently contains one backend service and one frontend app.
- There is no separate `core-service`, `event-service`, `infra/` root module, or Redis-backed session service in this codebase.
- Runtime dependencies in the current code are PostgreSQL, Cloudflare R2 for media storage, and the Strava API.
- Authentication is JWT bearer auth. The frontend stores the token client-side and sends it through the `Authorization` header.

## Backend Architecture

### Package Layout

#### `api/`

HTTP entrypoints and DTO mapping live here.

- `AuthController`
- `UserController`
- `PostController`
- `MediaController`
- `StravaController`
- `Health`
- `api/dtos/*` for request and response payloads

#### `domain/`

The backend is feature-oriented. Each feature package keeps its entity, repository, and service together.

- `user/`
- `post/`
- `comment/`
- `follow/`
- `reaction/`
- `post_photo/`
- `post_view/`
- `strava/`

Important nuance: this is not a strict framework-free domain layer. The `domain/*` packages contain JPA/Panache entities and repositories alongside business services. Documentation and code should treat this as a pragmatic layered monolith, not pure DDD.

#### `application/strava/`

`StravaService` is the only application-level orchestration service at the moment. It coordinates OAuth callback handling, token refresh, persistence, and activity fetches.

#### `infra/`

External adapters and integration-specific helpers.

- `infra/postgres/StravaRepository` for Strava persistence lookups
- `infra/strava/*` for OAuth exchange, activity fetch, token refresh, and Strava DTOs
- `infra/storage/s3/R2StorageService` for Cloudflare R2 uploads/deletes
- `infra/storage/services/MediaService` for image upload orchestration

#### `utils/`

Cross-cutting helpers that do not fit a feature module.

- `JwtTokenService`
- `PasswordHash`
- `ImageProcessor`

### Backend Request Flow

The normal request path is:

```text
HTTP request
  -> controller in api/
  -> service in domain/ or application/
  -> repository / infra adapter
  -> DTO mapping
  -> JSON response
```

Transactions are mostly handled at the service layer, with a few controller-level `@Transactional` entrypoints for multipart flows.

### Key Backend Flows

#### Feed, search, and post creation

- `PostController` delegates to `PostService`.
- `PostService` uses `PostRepository` for pagination and ordering.
- Feed queries fetch ordered post IDs first, then reload entities with relations. This preserves sort order while fetching author/photos in one pass.
- `PostService.createPost()` fires `PostCreatedEvent` asynchronously after persistence.

#### Comments and reactions

- Comment and reaction endpoints are still under `PostController`.
- The actual logic lives in `CommentService` and `ReactionService`.
- Replies are implemented as self-referential comments with a single-level depth rule enforced in the service layer.

#### Media uploads

- `MediaController` validates file type and size.
- `PostPhotoService` and `MediaService` coordinate ownership checks and storage.
- `R2StorageService` writes bytes to Cloudflare R2.
- Post/photo metadata remains in PostgreSQL.

#### Strava integration

- `StravaController` exposes connect, callback, disconnect, and recent activity endpoints.
- `application/strava/StravaService` coordinates the flow.
- External communication happens through `infra/strava/StravaOAuthClient`, `StravaApiClient`, and `StravaTokenRefresher`.
- Connection state is stored in `strava_integrations`.

#### Authentication

- `AuthController` handles registration, login, and `/api/auth/me`.
- JWTs are generated by `JwtTokenService`.
- Authenticated controllers extract the current user ID from `SecurityContext`.

### Persistence and Migrations

Schema changes are owned by Flyway migrations in `backend/src/main/resources/db/migration`.

Current migrations:

- `V1__init.sql`
- `V2__add_comment_replies.sql`
- `V3__add_post_photos.sql`
- `V4__remove_thumbnail_url.sql`
- `V5__add_post_views.sql`
- `V6__add_strava_integration.sql`

Main tables in the current schema:

- `users`
- `posts`
- `comments`
- `reactions`
- `follows`
- `post_photos`
- `post_views`
- `strava_integrations`

## Frontend Architecture

### Routing and Shell

Routes live in `frontend/src/routes`.

Current top-level pages:

- `/` - main feed
- `/explore`
- `/login`
- `/register`
- `/notifications`
- `/settings`
- `/[username]` - public profile page

`src/routes/+layout.svelte` is the main app shell. It does three important things:

- initializes auth on mount
- registers the global unauthorized handler
- switches between the desktop route tree and `MobileLayout` when running inside Telegram Mini App

### Frontend Package Layout

#### `lib/api/`

Low-level transport code.

- `client.ts` centralizes `fetch`, timeout handling, auth headers, JSON parsing, and 401 handling
- `endpoints.ts` holds the route map

#### `lib/services/`

Typed wrappers around backend endpoints.

- `auth.service.ts`
- `user.service.ts`
- `post.service.ts`
- `media.service.ts`
- `strava.service.ts`

Rule of thumb: route components and UI components should talk to services, not build URLs manually.

#### `lib/features/`

Stateful orchestration for user-facing features, implemented with Svelte 5 runes.

- `posts/useComments.svelte.ts`
- `posts/useComposer.svelte.ts`
- `posts/usePostMutations.svelte.ts`
- `posts/useReactions.svelte.ts`
- `profile/useProfile.svelte.ts`
- `profile/useProfilePage.svelte.ts`
- `explore/useSearch.svelte.ts`

Use this layer for page-specific state machines and async behavior that would make components too heavy.

#### `lib/stores/`

Cross-page state and caches.

- `auth.store.svelte.ts` handles optimistic auth boot, token storage, and logout
- `feed-cache.store.svelte.ts` implements stale-while-revalidate feed caching
- `profile-cache.store.svelte.ts` caches profile page payloads
- mobile UI stores keep track of tabs, overlays, and sheets
- `strava.store.svelte.ts` holds Strava connection state on the client

Use a store when multiple routes/components need the same state. Use `lib/features` when the state is scoped to one page or one feature flow.

#### `lib/components/`

UI is grouped by product area.

- `post/` for post card composition
- `profile/` for profile page UI
- `comments/` for thread UI
- `composer/` for post creation widgets
- `explore/` for search tabs/results
- `mobile/` for Telegram/mobile-specific shells and overlays
- root-level components for shared app pieces like `LeftSidebar` and `FeedComposeBox`

#### `lib/ui/`

Reusable low-level primitives such as:

- `Button`
- `Modal`
- `Sheet`
- `Tabs`
- `Toast`
- `Avatar`
- `Skeleton`

Put code here only if it is generic enough to be reused across multiple features.

#### `lib/query/`

Small in-house query primitives (`Query`, `Mutation`, cache helpers`) used instead of a full external data library.

#### `lib/schemas/`, `lib/types/`, `lib/utils/`, `lib/actions/`

- `schemas/` for Zod validation
- `types/` for shared request/response and domain types
- `utils/` for formatting and small helpers
- `actions/` for reusable DOM behavior such as click-outside, focus trap, and infinite scroll

#### `lib/telegram.ts` and `lib/tma/`

Telegram Mini App integration lives here.

- Telegram theme and viewport bridging
- CloudStorage helpers
- platform detection
- TMA-specific buttons/integration hooks

### Frontend Data Flow

The common frontend path is:

```text
route or component
  -> feature hook or store
  -> service
  -> api client
  -> backend
```

Typical examples:

- Feed page: `+page.svelte` -> `feedCacheStore` -> `postService` -> `apiClient`
- Profile page: `[username]/+page.svelte` -> `UseProfilePage` -> `userService`
- Comments UI: `PostComments.svelte` -> `UseComments` -> `postService`
- Media upload: `FeedComposeBox` / profile editor -> `mediaService` -> multipart API

### Testing

Frontend tests are split between:

- colocated unit tests in `src/lib/**`
- Playwright end-to-end tests in `src/tests/e2e`

Backend tests are split between:

- domain/repository tests in `backend/src/test/java/org/critiqal/domain`
- API/security integration tests in `backend/src/test/java/org/critiqal/api`

## Where New Code Should Go

### Backend

- New endpoint: add controller method in `api/` and DTOs in `api/dtos/`
- New business rule in an existing feature: extend that feature's `domain/<feature>/...`
- New third-party integration: put orchestration in `application/` only if it spans multiple adapters; otherwise keep it in the relevant feature package
- New external client or storage adapter: add it under `infra/`
- Schema change: add a new Flyway migration

### Frontend

- New API call: add it to the relevant file in `lib/services/`
- New cross-page cache or session state: `lib/stores/`
- New page-specific async state: `lib/features/`
- New reusable primitive: `lib/ui/`
- New feature-specific UI: `lib/components/<area>/`
- New route: `src/routes/<path>/`

## Build and CI Entry Points

### Backend

- `./mvnw test` - unit and repository tests
- `./mvnw verify` - integration suite

### Frontend

- `npm run check`
- `npm run lint`
- `npm run test`
- `npm run test:e2e`
- `npm run build`

### CI/CD

- `.github/workflows/ci.yml`
- `.github/workflows/deploy-dev.yml`
- `.github/workflows/deploy-prod.yml`
