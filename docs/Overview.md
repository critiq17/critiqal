# Critiqal — Overview

## What is Critiqal

Critiqal is a minimalist social network built around real-world activity. Users connect their accounts to GitHub and Strava, share progress with friends, and earn on-chain tokens for completing activities. Those tokens can be spent on in-app perks and features.

No algorithmic feed manipulation. No aggressive moderation. Just activity, friends, and rewards.

## Core Concept

- Post updates from GitHub (commits, PRs, repos) and Strava (runs, rides, workouts)
- Follow friends, react to their activity
- Earn `$CRIT` tokens automatically when activity thresholds are met
- Spend tokens on profile upgrades, badges, and future perks

## Target Users

Developers and athletes who want a low-noise space to share progress with people they actually know.

## Tech Stack

| Layer     | Technology                              |
|-----------|-----------------------------------------|
| Backend   | Java 21 + Quarkus                       |
| Frontend  | Svelte 5 + TypeScript                   |
| Database  | PostgreSQL 16                           |
| Cache     | Redis 7                                 |
| Messaging | Internal event-service (SSE/WebSocket)  |
| Auth      | Cookie-based sessions (HttpOnly, Secure)|
| Infra     | Docker Compose, Nginx, GitHub Actions   |

## Architecture Principles

- **DDD** — domain logic lives in the domain layer, not in services or controllers
- **SOLID** — every class has one reason to change; dependencies are injected, not created
- **DRY** — no duplicated logic; shared code extracted to libraries or modules
- **KISS** — simple solutions preferred; complexity must be justified

## Project Status

Early development. See `docs/Features.md` for the planned roadmap.