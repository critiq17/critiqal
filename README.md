<p align="center">
  <img src="docs/assets/logo.svg" width="72" height="72" alt="Critiqal" />
</p>

<h1 align="center">Critiqal</h1>

<p align="center">A minimalist social network built around real activity.<br/>No algorithms. No ads. No noise.</p>

---

## What it is

Critiqal is a small, quiet social network for developers and athletes. You connect GitHub and Strava, your real activity becomes your feed, and friends follow what you actually do — not what an algorithm decides to show.

The product is a Telegram Mini App. There is no native iOS or Android app yet; the Mini App is the intentional, temporary delivery mechanism while the product is still small. It runs full-screen inside Telegram and behaves like a native client, without the overhead of app store review cycles.

## Environments

| Environment | URL | Telegram bot |
|-------------|-----|--------------|
| Production  | [critiqal.xyz](https://critiqal.xyz) | [@critiqa1_bot](https://t.me/critiqa1_bot) |
| Development | [dev.critiqal.xyz](https://dev.critiqal.xyz) | [@critiqal_dev_bot](https://t.me/critiqal_dev_bot) |

Production is the live network. Development is a separate, fully isolated copy — its own database, its own bot, its own data — used to validate changes before they reach production. Open the relevant bot in Telegram and launch the Mini App from there.

## What works today

- Email and password accounts with HttpOnly cookie sessions
- Profiles, follow and unfollow, chronological feed
- Posts with text and photos, reactions, single-level comment replies
- Notifications for new followers and reactions
- Strava integration: OAuth connect, automatic posts from activities, weekly summary on the profile
- Telegram Mini App shell: native-feeling navigation, theme bridging, haptics, full-screen mode
- Stale-while-revalidate caching for feed and profiles

## What is coming

- A native GitHub integration alongside Strava: posts from commits, merged pull requests, and new repositories
- `$CRIT`, the network's own token: earned automatically for real activity, spent on profile perks
- Fast, low-cost on-chain transactions so balances can be claimed and moved without friction
- Direct messages between users — targeted for summer, possibly sooner

The token and messaging layers are designed but not yet shipped. The roadmap in [docs/Features.md](docs/Features.md) describes them in detail.

## Stack

- Backend: Java 21 + Quarkus 3, PostgreSQL 16, Redis-backed sessions
- Frontend: SvelteKit 2 + Svelte 5 + TypeScript
- Media: Cloudflare R2
- Integrations: Strava API, Telegram Mini App
- Infrastructure: Docker Compose, Nginx, Cloudflare Tunnel, GitHub Actions

## Documentation

- [docs/Overview.md](docs/Overview.md) — what the product is and how it is shaped
- [docs/Engineering.md](docs/Engineering.md) — repository layout, architecture, and where new code goes
- [docs/Features.md](docs/Features.md) — current features and the roadmap
- [docs/deploy.md](docs/deploy.md) — deployment and infrastructure runbook
