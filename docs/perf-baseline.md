# Performance Baseline — Phase 0

> Captured: 2026-05-30. Backend not running during measurement — API latency rows marked N/A.
> Re-run after each phase to validate improvement.

---

## 1. Backend API Latency

| Endpoint | p50 | p95 | Notes |
|---|---|---|---|
| GET /api/users/{username} | N/A | N/A | Backend offline during baseline |
| GET /api/users/{id}/stats | N/A | N/A | |
| GET /api/users/{id}/followers | N/A | N/A | O(N) — full list fetched to compute `isFollowing` |
| GET /api/users/{username}/posts | N/A | N/A | |
| GET /api/integrations/strava/public/{id} | N/A | N/A | |

**Action:** Re-measure once backend is running with `time curl -s http://localhost:8080/api/users/{username}`.

---

## 2. Bundle Sizes (after `npm run build`)

### Route nodes (client JS, uncompressed)

| Route | Node file | Raw | ~gzip |
|---|---|---|---|
| layout (shared) | node 0 | — | — |
| / (feed) | node 2 | 8.8 kB | ~3.5 kB |
| /[username] (profile) | node 3 | 15.1 kB | ~5 kB |
| /explore | node 4 | 4.0 kB | ~1.5 kB |
| /login | node 6 | 9.9 kB | ~3.5 kB |
| /settings | node 10 | **20.8 kB** | ~6.1 kB |

### Largest shared chunks

| Chunk | Raw | gzip | Loaded by |
|---|---|---|---|
| C3wAJKqX.js | 68.8 kB | 21.2 kB | layout (node 0) — all routes |
| CYa5Y7jG.js | 39.8 kB | 12.1 kB | layout + profile |
| shkPc5zi.js | 37.3 kB | 12.3 kB | layout + profile |
| C2kr83fG.js | 31.6 kB | 12.3 kB | layout (Svelte runtime) |
| K9Ecnpb4.js | 28.1 kB | 11.0 kB | layout + profile + settings |

**Total client JS:** 415 kB raw  
**Total client CSS:** 175 kB raw  
**Initial load estimate (layout deps, gzip):** ~100–120 kB JS + ~20 kB CSS before any route code

### Source file LOC (key files)

| File | LOC |
|---|---|
| MobileSettingsOverlay.svelte | 1294 |
| MobileCommentsSheet.svelte | 1273 |
| settings/+page.svelte | 1248 |
| MobileProfile.svelte | 680 |
| [username]/+page.svelte | 669 |
| MobileAuthScreen.svelte | 593 |
| MobilePostComposer.svelte | 521 |
| useProfilePage.svelte.ts | 324 |

---

## 3. Request Waterfall (profile page, cold load)

Sequential dependency layers:

| Layer | Requests | Blocking |
|---|---|---|
| 0 | Download HTML → download + parse JS bundle → boot Svelte → mount layout → mount page | YES |
| 1 | `onMount` → `GET /api/users/{username}` | YES (nothing starts before mount) |
| 2 | After layer 1 resolves: `GET /api/users/{id}/stats` + `GET /api/users/{id}/followers` (for isFollowing) | YES (sequential after layer 1) |
| 3 | `GET /api/users/{username}/posts` | Parallel with layer 1 (good) |
| 4 | `GET /api/integrations/strava/public/{id}` (via `$effect`) | After layer 1 |

**Key problems:**
- All fetches start in `onMount`, not in `load` — requests wait for full JS boot + component mount
- Layer 2 is serial after layer 1: at minimum 2 round-trips before stats/isFollowing render
- `GET /api/users/{id}/followers` returns the **entire followers list** just to check `isFollowing` — O(N) over follower count

**Estimated cold profile load:** bundle boot (~500ms) + 2 × network RTT (~200ms each) = **900ms–2s**

---

## 4. Structural Issues

- `ssr = false` in `+layout.ts` — pure SPA, no streaming, no preloading
- `load` functions return minimal data; all real fetching in `onMount`
- No `/follow/check` endpoint — entire followers list fetched for boolean
- MobileLayout (Telegram Mini App path) is a separate component tree bypassing SvelteKit routing — loaded eagerly in the layout chunk

---

## 5. Decision Gate

| Check | Status | Implication |
|---|---|---|
| Single backend endpoint >300ms? | Unknown (backend offline) | Re-check after backend measurement |
| Bundle boot dominant? | Likely secondary (JS is ~120 kB gzipped for initial load) | Phase 3 helps but Phase 1 is higher ROI |
| **Waterfall dominant?** | **YES** — 2 serial request layers + O(N) followers fetch | **Phase 1 is highest priority** |

**Conclusion:** The dominant costs are (1) requests starting only after SPA boot (`onMount` fetching), and (2) the 2-layer serial waterfall on profile open. **Phase 1 (kill the waterfall) is the highest-ROI change.**

---

## 7. Phase 2 Decision: SSR

**Task 2.1 (hover preloading):** Already implemented — `app.html` has `data-sveltekit-preload-data="hover"` and `data-sveltekit-preload-code="viewport"`. No change needed.

**Task 2.2 (SSR for profile route):** **Decision: stay SPA.**

Reasons:
- `app.html` runs Telegram SDK initialization synchronously (`window.Telegram.WebApp`, `tg.ready()`, `tg.expand()`, viewport events) — these are client-only and cannot run server-side
- Phase 1 removes the dominant bottleneck (serial waterfall + O(N) followers fetch); Phase 2.1 preloads code on viewport entry — together these deliver most of the target improvement
- No safe way to scope SSR to desktop-only without structural changes to the Telegram detection (which is client-side)

---

## 6. Targets

| Metric | Baseline | Target |
|---|---|---|
| Profile cold load (perceived) | 1–2 s | ≤500 ms |
| Profile serial request layers | 2 | 1 |
| Followers fetch on profile open | YES (O(N)) | NO |
| Total client JS (raw) | 415 kB | <300 kB (after Phase 3) |
| Settings node chunk | 20.8 kB | <10 kB (lazy-load overlays) |
