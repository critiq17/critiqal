# Critiqal

A minimalist social network for developers and athletes. Connect GitHub and Strava, share progress with friends, earn `$CRIT` tokens for real activity.

No algorithms. No ads. No noise.

---

## Features

**Core Social**

- Profiles, follow/unfollow, chronological feed
- Post text updates manually or auto-sync from integrations
- Reactions and comments
- Notifications: followers, reactions

**GitHub Integration**

- OAuth2 connect
- Auto-post on commits, merged PRs, new repos
- Contribution stats on profile

**Strava Integration**

- OAuth2 connect
- Auto-post on runs, rides, swims, workouts
- Weekly activity summary on profile

**Token Rewards ($CRIT)**

| Activity               | Reward    |
| ---------------------- | --------- |
| Post per day           | 1 $CRIT   |
| GitHub commit synced   | 2 $CRIT   |
| Strava activity synced | 3 $CRIT   |
| 7-day streak           | +10 $CRIT |
| Follower milestone     | 5 $CRIT   |

Spend on: profile badges, extended bio, follower suggestions priority.

**On-Chain (Phase 4)**

- EVM wallet connect
- Claim in-app $CRIT balance to on-chain wallet
- Token transaction history

---

## Stack

- **Backend** — Java 21 + Quarkus 3, PostgreSQL 16
- **Frontend** — SvelteKit 2 + Svelte 5 + TypeScript
- **Media / Integrations** — Cloudflare R2, Strava API
- **Infra** — Docker Compose, GitHub Actions

For the current repository layout and code boundaries, see `docs/Engineering.md`.
