# Critiqal — Features

## Phase 1 — Core Social

- User registration and login (email + password, cookie session)
- User profiles (avatar, bio, links)
- Follow / unfollow users
- Post text updates
- Comments with single-level replies
- Feed: chronological posts from followed users
- Notifications: new follower, post reaction

## Phase 2 — Integrations

### GitHub

- OAuth2 connect
- Auto-post on: new commit, merged PR, new repo
- Display contribution stats on profile

### Strava

- OAuth2 connect
- Auto-post on: activity completed (run, ride, swim, workout)
- Display weekly activity summary on profile

## Phase 3 — Token Rewards ($CRIT)

Token earning rules (configurable):

| Activity                  | Reward       |
|---------------------------|--------------|
| Post per day              | 1 $CRIT      |
| GitHub commit synced      | 2 $CRIT      |
| Strava activity synced    | 3 $CRIT      |
| 7-day activity streak     | 10 $CRIT bonus |
| New follower milestone    | 5 $CRIT      |

Token spending:

- Custom profile badge
- Extended bio length
- Priority in follower suggestions
- Future: marketplace perks

Anti-abuse:

- Daily earning caps per user
- Rate limiting on sync endpoints
- Cooldown between same-source events

## Phase 4 — On-Chain

- Wallet connect (EVM-compatible)
- $CRIT token contract (ERC-20 or similar)
- Claim in-app balance to on-chain wallet
- Token history and transaction log

## Phase 5 — Polish

- Real-time feed updates via SSE
- Dark / light theme
- Mobile-responsive layout
- Push notifications (web push)
- Public API for third-party integrations

## Out of Scope (v1)

- Algorithmic feed ranking
- Advertising
- Content reporting / heavy moderation
- Direct messages
- Groups or communities
