# Admin Frontend — ready-to-build spec (paste into a fresh session)

> Backend is DONE. Build ONLY the frontend: a **separate SvelteKit app** for the admin panel.
> Branch: `feat/admin-dashboard`. Conventions: Svelte 5 runes, strict TS (no `any`, interfaces for objects),
> files <300 lines, theme tokens only (no hardcoded colors). TDD. Do NOT commit.

## Goal (user's intent)
Admin panel in the social-network style: search **people** and **posts**, **grant a badge** to a user by
selecting them and confirming (and revoke). Show avatars, names, @usernames. Minimalist but high quality, no bugs.

## Placement decision
**Separate SvelteKit application** (new project, e.g. `admin-frontend/`), intended to be served at
`admin.critiqal.xyz` (nginx → `127.0.0.1:3002`, proxying `/api/*` → backend `127.0.0.1:8082`).
Mirror the existing `frontend/` config (adapter-node, vite 7, svelte 5.54, ssr=false SPA, vitest + testing-library).

## Backend — ALREADY EXISTS (do not change). Exact contracts:

Auth (public, cookie `admin_session`, SameSite=STRICT, HttpOnly, path=/):
- `POST /api/admin/auth/login` `{username,password}` → **202** `{challengeToken, method:"TOTP"}`; **404** on bad creds / not-admin / 2FA-not-enabled (same 404 for all → don't reveal admin existence).
- `POST /api/admin/auth/2fa` `{challengeToken, code}` → **200** `{ok:true}` + sets `admin_session` cookie; **401** `{error:"Invalid code"}`; **404** if challenge invalid.
- `POST /api/admin/auth/logout` → **204** + clears cookie.

Protected (`@RolesAllowed("ADMIN")`; **denials are masked as 404**, not 401/403):
- `GET /api/admin/me` → `{admin:true}`.
- `GET /api/admin/users/search?q=&page=&size=` → `Page<AdminUserDTO>`. Blank `q` = browse recent users.
- `GET /api/admin/users/{userId}` → `AdminUserDTO`.
- `GET /api/admin/posts/search?q=&page=&size=` → `Page<PostDTO>`. Blank `q` = latest feed.
- `GET /api/admin/badges` → `AdminBadgeDTO[]` (all grantable badges).
- `POST /api/admin/users/{userId}/badges` `{code}` → `{granted,user}` (idempotent).
- `DELETE /api/admin/users/{userId}/badges/{code}` → `{revoked,user,removed}`.

### JSON shapes
- `Page<T>` = `{ content: T[], page: number, size: number, total: number, hasNext: boolean }`.
- `PageRequest`: query params `page` (default 0), `size` (default 20, max 50).
- `AdminUserDTO`: **READ** `backend/src/main/java/org/critiqal/api/admin/response/AdminUserDTO.java` for exact fields.
  Expect at least `{ id, username, name, avatarUrl, badges: UserBadgeDTO[] }`.
- `AdminBadgeDTO`: **READ** `backend/src/main/java/org/critiqal/api/admin/response/AdminBadgeDTO.java`.
  Expect `{ code, name, description, iconUrl? }`.
- `UserBadgeDTO`: **READ** `backend/src/main/java/org/critiqal/api/badge/response/UserBadgeDTO.java`.
  Frontend shape used elsewhere: `{ id, code, name, description, iconUrl|null, metadata, awardedAt }`.
- `PostDTO` = `{ id, author: UserDTO, content, photos:[{id,url,position}], viewCount, likeCount, likedByMe, status, createdAt }`.
  `UserDTO` = `{ id, username, name, bio, avatarUrl, email, badges:UserBadgeDTO[], emailVerified, pendingEmail, twoFactorEnabled, createdAt, stats, isFollowing }`.
- `BadgeCode` enum (revoke uses the code string): `ORIGIN | CENTURION | GLADIATOR | LEGATUS | SCRIBE | ORATOR | TRIBUNE`.

> First step in the new session: read the 3 admin DTO files above + `backend/src/main/java/org/critiqal/domain/badge/BadgeCode.java` to lock exact field names. That's the only backend reading needed.

## CRITICAL frontend gotchas
- All admin calls send `credentials:'include'` (cookie). The existing `frontend/` apiClient calls a global
  `onUnauthorized` on 401 that logs the main user out — in the **separate app** that handler doesn't exist, so
  just build a clean apiClient. But still: admin `GET` denials return **404** (not 401), and `2fa` returns **401**
  on bad code — handle 401 locally on the login screen only, never globally log-out-redirect loop.
- Treat any `ApiError 404` on login as generic "Invalid credentials".
- The admin app is desktop-style. No Telegram/MobileLayout. Build its own minimal shell + guard.

## Reuse from existing `frontend/` (copy/adapt, don't import across apps)
- apiClient pattern: `frontend/src/lib/api/client.ts` (get/post/put/delete, credentials include, ApiError, timeout).
- Theme tokens (copy the `:root` block from `frontend/src/routes/+layout.svelte`): `--color-bg #0c0c0c`,
  `--color-surface #141414`, `--color-surface-raised #1e1e1e`, `--color-border #242424`,
  `--color-text-primary #eaeaea`, `--color-text-secondary #8c8c8c`, `--color-text-muted #575757`,
  `--color-accent #e05252`, radii `--radius-sm/md/lg/full`, glass `--glass-bg/--glass-border/--glass-blur`.
- Search UI feel (debounce 300ms, Posts/People tabs): `frontend/src/routes/explore/+page.svelte`.
- User row (avatar + name + @username + initial fallback): `frontend/src/lib/components/explore/ExplorePeopleTab.svelte`.
- Badge visuals: `frontend/src/lib/components/badges/badgeMeta.ts` (BADGE_META, tierStyle) and `BadgePill.svelte`
  (port a simplified BadgePill for rendering a user's badges and the grant selector).
- Avatar fallback: first letter of `name ?? username`, uppercased, in a round div using theme tokens.

## Files to create (separate app under `admin-frontend/`)
Config: `package.json`, `svelte.config.js`, `vite.config.ts` (server.port 3002, proxy `/api`→`http://127.0.0.1:8082`),
`tsconfig.json`, `app.html`, `.gitignore`, eslint/prettier (optional), `vitest` config.

App:
- `src/lib/api/client.ts` — apiClient (credentials include, ApiError, no global logout handler).
- `src/lib/api/endpoints.ts` — `API.admin.{login,login2fa,logout,me,usersSearch,user(id),postsSearch,badges,grantBadge(id),revokeBadge(id,code)}`.
- `src/lib/types/index.ts` — AdminUser, UserBadge, AdminBadge, Post, PageResponse<T>, ApiError, TwoFactorChallenge.
- `src/lib/services/admin.service.ts` — login, verifyTwoFactor, me, searchUsers(q,page), getUser(id), searchPosts(q,page), listBadges, grantBadge(userId,code), revokeBadge(userId,code), logout.
- `src/lib/stores/admin-auth.store.svelte.ts` — `{status:'unknown'|'authed'|'anon'}` via `me()`.
- `src/routes/+layout.ts` (ssr=false, prerender=false), `+layout.svelte` (global theme + admin shell + logout).
- `src/routes/login/+page.svelte` — step1 username+password → step2 TOTP → goto('/').
- `src/routes/+layout` guard or `+page` guard: on mount call `me()`; if it throws → goto('/login').
- `src/routes/+page.svelte` — dashboard, tabs People/Posts (debounced search).
- `src/lib/components/AdminUserRow.svelte`, `AdminPostRow.svelte`, `GrantBadgePanel.svelte`
  (select badge → confirm → grant; show current badges with revoke), `AdminBadgePill.svelte`, `Toast.svelte`.
- Handle loading / empty / error states everywhere. Refetch user's badges after grant/revoke. Optimistic + toast.

## Tests (vitest + @testing-library/svelte)
- `admin.service` unit tests (mock fetch/apiClient): each method hits correct path/method/body, parses Page shape.
- `admin-auth` store: me() → authed/anon transitions.
- Component test for grant flow: select badge → confirm → success state; revoke removes a badge.

## Deliverable / report
Run `cd admin-frontend && npm install && npm run check && npm test`. Report PASS/FAIL + file list. Do NOT commit.
