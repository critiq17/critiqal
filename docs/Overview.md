<p align="center">
  <img src="assets/logo.svg" width="56" height="56" alt="Critiqal" />
</p>

# Critiqal — Overview

## What Critiqal is

Critiqal is a minimalist social network built around real-world activity. People connect their GitHub and Strava accounts, share progress with friends, and earn the network's token for activity they actually do. There is no algorithmic feed, no advertising, and no aggressive moderation — only activity, people, and rewards.

## Core concept

- Posts come from real activity: GitHub commits, pull requests, and repositories; Strava runs, rides, and workouts.
- People follow friends and react to their progress.
- `$CRIT`, the network token, is earned automatically when activity thresholds are met.
- The token is spent on profile upgrades, badges, and future perks.

## How it is delivered

Critiqal ships as a Telegram Mini App. There is no native iOS or Android application yet. The Mini App is a deliberate, temporary delivery mechanism: it runs full-screen inside Telegram, behaves like a native client, and removes the cost of app store review while the product is still small. The desktop web experience is served from the same codebase.

| Environment | URL | Telegram bot |
|-------------|-----|--------------|
| Production  | critiqal.xyz | @critiqa1_bot |
| Development | dev.critiqal.xyz | @critiqal_dev_bot |

Development is a fully isolated mirror of production with its own database and bot, used to validate changes before release.

## Target users

Developers and athletes who want a low-noise space to share progress with people they actually know.

## Tech stack

| Layer        | Technology                               |
|--------------|------------------------------------------|
| Backend      | Java 21 + Quarkus 3                       |
| Frontend     | SvelteKit 2 + Svelte 5 + TypeScript       |
| Database     | PostgreSQL 16                            |
| Sessions     | Redis-backed HttpOnly cookie sessions     |
| Media        | Cloudflare R2                            |
| Integrations | Strava API, Telegram Mini App             |
| Infrastructure | Docker Compose, Nginx, Cloudflare Tunnel, GitHub Actions |

## Architecture principles

- Layered monolith — one Quarkus API and one SvelteKit app in a single repository.
- Feature-oriented packages — backend code is grouped by feature (`post`, `user`, `comment`, and so on).
- Pragmatic domain packages — `domain/*` may hold Panache entities, repositories, and services together.
- Thin transport layers — controllers and frontend services stay close to HTTP concerns.
- Pragmatic boundaries — entities, repositories, and services can share a feature package when that keeps the code simpler.

## What works today

Accounts, profiles, following, the chronological feed, posts with photos, reactions, comment replies, notifications, the Strava integration, and the Telegram Mini App shell are all in production.

## What is planned

A native GitHub integration, the `$CRIT` token with automatic earning and spending, fast low-cost on-chain transactions for claiming and moving balances, and direct messages between users — messaging is targeted for summer. See `docs/Features.md` for the full roadmap and `docs/Engineering.md` for the current code structure.
