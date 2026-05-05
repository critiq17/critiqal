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

| Layer        | Technology                              |
|--------------|-----------------------------------------|
| Backend      | Java 21 + Quarkus 3                     |
| Frontend     | SvelteKit 2 + Svelte 5 + TypeScript     |
| Database     | PostgreSQL 16                           |
| Media        | Cloudflare R2                           |
| Integrations | Strava API                              |
| Auth         | HttpOnly cookie sessions backed by Redis |
| Infra        | Docker Compose, GitHub Actions          |

## Architecture Principles

- **Layered monolith** — one Quarkus API and one SvelteKit app in a single repo
- **Feature-oriented packages** — backend code is grouped by feature (`post`, `user`, `comment`, etc.)
- **Pragmatic domain packages** — `domain/*` may contain Panache entities, repositories, and services together
- **Thin transport layers** — controllers and frontend services stay close to HTTP concerns
- **Pragmatic boundaries** — repositories, entities, and services can live in the same feature package when that keeps the code simpler

## Project Status

Active development. See `docs/Engineering.md` for the current code structure and `docs/Features.md` for the broader roadmap.
