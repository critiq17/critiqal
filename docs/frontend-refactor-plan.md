# Frontend Refactor & Performance Plan

> **Audience:** an implementing agent (e.g. Sonnet) in a fresh session.
> **Read this file top-to-bottom, then execute one phase at a time.** Do not batch phases.
> Each phase has: Goal → Tasks → Files → Risk → Validation → Acceptance. Stop and report after each phase.

---

## 0. Mission & Hard Constraints

**Goals (in priority order):**
1. **Speed.** Page loads, especially the profile page, must reach ~200 ms perceived (500 ms worst case). Today profile is 1–2 s.
2. **Quality.** Remove duplication and bad patterns; apply current best practices.
3. **Less code, fewer files.** Same functionality, fewer lines, fewer files.

**NON-NEGOTIABLE constraints:**
- **The visual design / UI MUST NOT change.** Not pixels, not spacing, not animations, not copy. Any change is allowed ONLY if the rendered result is provably identical or strictly better. When in doubt, don't.
- Behaviour and functionality must not regress.
- Owner authorised changes to **both frontend and backend** (Java/Quarkus). Backend changes are allowed where they remove a frontend waterfall.
- SSR may be enabled **if proven safe for the Telegram Mini App path**. Decide with measurements, not assumptions (Phase 2).

**Process rules:**
- TDD where logic changes (per repo CLAUDE.md). Visual/markup changes are guarded by visual regression (Phase 1, task 1.0), not unit tests.
- Route any backend work through the `critiqal-java` agent; frontend through `critiqal-svelte`; tests through `critiqal-tester`; commits through `critiqal-git`. One focused commit per phase/sub-feature.
- After every phase: run `npm run check`, `npm run lint`, `npm run test`, and the visual-regression suite. All green before moving on.

---

## 1. Diagnosis (grounded, with file references)

### 1.1 Why it's slow

**A. Pure SPA — no SSR, no preloading.**
- `src/routes/+layout.ts` sets `export const ssr = false; export const prerender = false;`.
- Cold path is fully serial: download `index.html` → download + parse the JS bundle → boot Svelte → mount layout → mount page → *only then* fetch data. Nothing is streamed; the user sees a skeleton until both boot **and** the request waterfall finish.

**B. `load` functions are empty — data fetching is deferred to `onMount`.**
- `src/routes/[username]/+page.ts` and `src/routes/+page.ts` return only `{ }` / `{ username }`.
- Real fetching happens inside components in `onMount` (e.g. `[username]/+page.svelte` → `UseProfilePage.loadProfile()`). So requests start *after* the component code loads and mounts, instead of in parallel with code download (SvelteKit starts `load` as soon as it knows the route).

**C. Request waterfall on the profile page.** In `src/lib/features/profile/useProfilePage.svelte.ts → loadProfile()`:
1. `GET /api/users/{username}` (profile) — must resolve first.
2. In parallel after step 1 resolves: `GET /api/users/{id}/stats`, and `refreshIsFollowing()` →
   **`GET /api/users/{id}/followers` — fetches the ENTIRE followers list just to compute `isFollowing`.** The code itself flags this: *"backend should expose /follow/check"*. This is O(N) over followers on every profile open.
3. `GET /api/users/{username}/posts?page=0` (parallel with profile — good).
4. `GET /api/integrations/strava/public/{id}` via an `$effect` after profile resolves.

So a cold profile open = bundle boot + at least 2 sequential round-trip layers + a needlessly heavy followers fetch.

**D. Unknown: backend latency.** 1–2 s could be partly server-side. **Do not guess — Phase 0 measures it.**

### 1.2 Duplication & file bloat

- **Two parallel apps.** `src/routes/+layout.svelte` picks `isMobile = isTelegramMiniApp()`; when true it renders `MobileLayout` (a self-contained SPA using `mobile-*` stores + `nav-stack`), bypassing the route tree entirely. Desktop uses `src/routes/**`. Result: near-duplicate pairs:
  - `mobile/MobileSettingsOverlay.svelte` (1294 LOC) ↔ `settings/+page.svelte` (1248)
  - `mobile/MobileProfile.svelte` (680) + `mobile/ProfileOverlayView.svelte` + `mobile/OverlayProfileInfo.svelte` ↔ `[username]/+page.svelte` (669)
  - `mobile/MobileFeed.svelte` (334) ↔ `+page.svelte` (287)
  - `mobile/MobileExplore.svelte` ↔ `explore/+page.svelte`
  - `mobile/MobileCommentsSheet.svelte` (1273) ↔ `comments/*`
- `~14k LOC` in `src/lib/components` alone.
- **Corrupted/dead markup** in `src/routes/+layout.svelte` (repeated empty `interface Props {}`, stray empty `}` blocks around lines 30–48). Clean this.
- The shared core (services, `features/*` hooks, types, stores) is mostly already shared and well-structured — duplication is concentrated in the **view layer**, not the data layer. This is the key insight: we can de-duplicate *logic* aggressively without touching the two distinct *UIs*.

### 1.3 What's already good (don't rip out)
- SWR caching exists: `profile-cache.store`, `feed-cache.store`, `utils/persistentCache.ts` (session/localStorage, schema-versioned, SSR-guarded).
- `deviceId.ts` and `persistentCache.ts` already guard `window`/`localStorage` with `typeof`/try-catch → SSR-safe.
- `features/*` hooks already isolate page logic from components.
- API client is centralised (`api/client.ts`) with timeout, 401/email-verification handling.

---

## 2. Phases

### Phase 0 — Baseline measurement (no code changes yet) ⏱️ blocker for the rest
**Goal:** Replace guesses with numbers. Decide whether the bottleneck is backend latency, bundle boot, or the waterfall.

**Tasks:**
1. Measure each profile API endpoint server-side latency (curl/`time` against the running backend, warm and cold): `GET /api/users/{username}`, `/stats`, `/followers`, `/posts`, strava public. Record p50/p95.
2. Build the app (`npm run build`) and record bundle sizes per route chunk; identify the largest chunks (expect mobile overlays).
3. In a browser, capture a cold profile load waterfall (DevTools Performance + Network): time-to-first-byte, bundle parse/boot, first contentful paint, time-to-data-rendered. Note which requests are serialised.
4. Write findings into `docs/perf-baseline.md` (a table). This is the yardstick for every later phase.

**Acceptance:** `docs/perf-baseline.md` exists with concrete numbers and a one-line conclusion on the dominant cost.

> **Decision gate:** If a single backend endpoint is >300 ms, Phase 1 backend work is mandatory and highest-ROI. If bundle boot dominates, prioritise Phase 3. If the waterfall dominates, Phases 1–2.

---

### Phase 1 — Kill the request waterfall (biggest, safest speed win)
**Goal:** Profile renders all above-the-fold data from **one** dependency layer instead of three. No UI change.

**Task 1.0 — Set up the visual-regression guardrail FIRST.**
- Add Playwright screenshot tests (`@playwright/test` is already a dep) for the key screens at desktop + mobile widths: home feed, profile (own + other), explore, settings, a post with comments. Capture baselines from the CURRENT `master`/`feat/badges` build before any change.
- These snapshots are the contract that "design must not change." Every subsequent phase must keep them green (or the diff must be a deliberate, owner-approved improvement).
- Files: `src/tests/e2e/visual/*.spec.ts`, baseline PNGs committed.

**Task 1.1 — Backend: consolidate the profile payload.** (`critiqal-java`)
- Include `stats` (postsCount/followersCount/followingCount), `badges`, and `isFollowing` (relative to the authenticated viewer) **inside** the `GET /api/users/{username}` response. `badges` is already returned today; add `stats` + `isFollowing`.
- Add `GET /api/users/{id}/follow/check` (or fold `isFollowing` into the profile response and drop the separate call). Either way, **eliminate the full `getFollowers` fetch from the profile hot path.**
- Keep the standalone `/stats`, `/followers`, `/following` endpoints (still used by the followers modal and elsewhere). This is additive — no breaking change.
- Tests: backend unit/integration for the enriched payload.

**Task 1.2 — Frontend: collapse `loadProfile` to a single request.** (`critiqal-svelte`)
- In `useProfilePage.svelte.ts`, read `stats`, `badges`, `isFollowing` straight off the profile response. Delete `refreshIsFollowing()` and the eager `loadStats()` waterfall from the hot path (keep `loadStats` only as a fallback if the consolidated field is absent, or remove entirely once backend ships).
- Keep posts fetch in parallel (already is). Keep strava as a deferred, non-blocking enhancement.
- Apply the same consolidation to the mobile profile path (`MobileProfile` / `OverlayProfileInfo` use the same hook? verify; if they use a separate hook, share `useProfilePage`).

**Task 1.3 — Move fetch kickoff into `load`.**
- Convert `[username]/+page.ts` (and `+page.ts` for the feed) to start the data fetch in the `load` function using SvelteKit's `fetch`, returning a streamed promise so the component renders the shell immediately and resolves data as it arrives. The component keeps its current skeleton states. This starts the request during navigation, in parallel with chunk loading, instead of after mount.
- Preserve SWR: `load` should hand off to / cooperate with the existing cache stores, not bypass them.

**Risk:** Medium. The profile hook is shared logic; changing it touches both desktop and mobile. Mitigate with Task 1.0 snapshots + the existing `features/posts/*.test.ts` pattern (add `useProfilePage` tests).

**Validation:** `docs/perf-baseline.md` re-measured — profile cold load should drop by one full round-trip layer and shed the followers fetch. Visual snapshots green.

**Acceptance:** Profile above-the-fold data comes from ≤1 dependency layer; no `/followers` call on profile open; snapshots unchanged.

---

### Phase 2 — First-paint: SSR / preload (measure-gated)
**Goal:** Cut time-to-first-contentful-paint by not waiting for full SPA boot. **Only proceed per the Phase 0 decision gate and only if Telegram-safe.**

**Task 2.1 — Add hover/viewport preloading (low risk, do regardless).**
- Add `data-sveltekit-preload-data="hover"` on the app shell (or per-link) so desktop navigations prefetch code + `load` data before click. This alone makes intra-app navigation feel instant and is zero-risk for Telegram (no hover there, simply inert).

**Task 2.2 — Evaluate SSR for public, read-heavy routes (profile).**
- Spike: enable `ssr = true` for `[username]` only (per-route override), keep `+layout.ts` SSR off for the Telegram path, or branch SSR by request (Telegram detection happens client-side, so server must render a safe, auth-agnostic shell).
- **Hard checks before committing:**
  - Cookie auth: SvelteKit server `fetch` must forward the session cookie to Quarkus (same-origin via Nginx). Verify the profile endpoint works server-side; public profile data should render without the `X-Device-Id` header (which lives in localStorage and is unavailable server-side — confirm backend treats it as optional for GETs).
  - Telegram: the TMA boot (`initTelegram`, `window.Telegram`) must not run on the server. Ensure `MobileLayout` and `telegram.ts` stay client-only (they already guard `window`). The server-rendered HTML must hydrate cleanly into the SPA the TMA expects.
  - No hydration mismatch on theme/locale (`document.documentElement.lang`, theme tokens).
- If any check is shaky, **stop and keep SPA** — Phase 1 + 2.1 already deliver most of the win. Record the decision in `docs/perf-baseline.md`.

**Risk:** High if SSR is forced onto the Telegram path. Low if scoped to desktop public routes or skipped.

**Acceptance:** Either (a) SSR enabled on profile with verified Telegram safety and FCP improved, or (b) a written, evidence-based decision to remain SPA. Plus 2.1 shipped.

---

### Phase 3 — Bundle size & lazy loading
**Goal:** Shrink boot cost. Heavy, rarely-first-seen UI must not be in the initial chunk.

**Tasks:**
1. Lazy-load the giants via dynamic `import()` so they're not in the critical path: `MobileSettingsOverlay` (1294), `MobileCommentsSheet` (1273), `MobilePostComposer`, `FollowersModal`, `ProfileEditOverlay`, language/sign-up modals. They render only on interaction → perfect for `{#await import(...)}` or `$effect`-gated dynamic import.
2. Confirm SvelteKit route-based splitting is effective post-Phase-1; check no barrel file (`components/post/index.ts`, `ui/index.ts`, `services/index.ts`) is forcing eager inclusion of unused modules. Prefer direct imports if a barrel pulls a heavy graph.
3. Audit dependencies actually shipped to the client (zod, i18n locale bundles). Lazy-load non-active locale files (`i18n/locales/{en,ru,uk}.ts`) — load only the active locale, fetch others on switch.

**Risk:** Low–medium (dynamic imports can flash if not guarded by the existing skeletons — reuse them).

**Validation:** Initial route chunk shrinks vs Phase 0 numbers; snapshots green; overlays still open instantly enough (preload on intent).

**Acceptance:** Largest overlays out of the initial chunk; only active locale shipped initially.

---

### Phase 4 — De-duplicate logic (less code, same two UIs)
**Goal:** Remove duplicated *logic* between mobile and desktop while keeping both *views* pixel-identical.

**Principle:** Share hooks/stores/services/utils, NOT markup. The two UIs stay separate Svelte components; their brains become one.

**Tasks:**
1. **Profile:** ensure desktop `[username]/+page.svelte` and `mobile/MobileProfile.svelte` both drive off a single `useProfilePage` hook (Phase 1 likely already forces this). Remove any parallel fetching/state logic in the mobile variant.
2. **Settings:** extract a `features/settings/useSettings.svelte.ts` containing all the state/mutations currently duplicated across `settings/+page.svelte` (1248) and `MobileSettingsOverlay` (1294). Both components keep their own markup but call the shared hook. Target: each file drops by hundreds of LOC.
3. **Feed / Explore / Comments:** same treatment — one hook per feature, two thin view components.
4. Consolidate duplicated small utilities and any copy-pasted formatting/validation into `lib/utils` / `lib/schemas` (single source).

**Risk:** Medium. Behaviour parity is the trap. Each extraction is a separate commit with its own snapshot + behaviour check.

**Validation:** LOC down measurably; snapshots green; manual behaviour parity on both desktop and TMA.

**Acceptance:** No feature has two independent logic implementations; documented LOC reduction.

---

### Phase 5 — Dead code & file reduction
**Goal:** Fewer files, less code, zero behaviour change.

**Tasks:**
1. Run `npx knip` (or `ts-prune` + `depcheck`) to find unused exports, files, and deps. Delegate to `refactor-cleaner` agent.
2. Remove the corrupted empty blocks in `+layout.svelte` (lines ~30–48) and any dead `interface Props {}` / stray braces.
3. Delete `.gitkeep` files in now-populated dirs; merge tiny single-use components into their parent where it doesn't hurt cohesion (respect the repo's "many small files" rule — only merge true fragments).
4. Remove orphaned stores/services flagged by knip.

**Risk:** Low, but verify each deletion isn't used by the Telegram path (which the route tree doesn't reference statically).

**Validation:** `npm run check`/`lint`/`test` green; snapshots green; bundle unchanged or smaller.

**Acceptance:** Knip reports no unused files/exports (or an explicit allowlist); file count reduced.

---

### Phase 6 — Lock it in (guardrails so it doesn't regress)
**Tasks:**
1. Wire the Phase-1 visual-regression suite into CI (`.github/workflows`), so any future design drift fails the build. (`critiqal-infra`)
2. Add a performance budget check: bundle-size ceiling per route + a Lighthouse/profile-load assertion in CI.
3. Update `docs/Engineering.md` with the new data-loading conventions (load functions + SWR + shared hooks) so future code follows the pattern.

**Acceptance:** CI fails on visual diff or bundle-budget breach; conventions documented.

---

## 3. Execution order & ROI summary

| Phase | Effort | Speed ROI | Risk | Gate |
|------|--------|-----------|------|------|
| 0 Measure | S | — | none | do first |
| 1 Kill waterfall (+ backend payload) | M | **High** | Med | core win |
| 2 SSR/preload | S–L | High (FCP) | High if forced on TMA | measure-gated |
| 3 Bundle/lazy | M | Med–High | Low | after 1 |
| 4 De-dup logic | L | Low (code health) | Med | independent |
| 5 Dead code | S–M | Low | Low | independent |
| 6 Guardrails | S | — | none | last |

**If only one thing ships: Phase 1.** It removes a full network layer and the O(N) followers fetch — the most likely cause of the 1–2 s profile load — with a clear visual-regression safety net.

## 4. Definition of done (whole effort)
- Profile cold load ≤ 500 ms (target ~200 ms warm), measured against `docs/perf-baseline.md`.
- Visual snapshots identical to the pre-refactor baseline across desktop + Telegram widths.
- Measurable LOC and file-count reduction; no duplicated feature logic; knip-clean.
- CI enforces visual + perf budgets.
