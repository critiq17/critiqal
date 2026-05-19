# Critiqal — Features

This document tracks what is shipped and what is planned. "Shipped" means it is live in production; "planned" means it is designed but not yet released.

## Shipped

### Accounts and social core

- Email and password registration and login, Redis-backed cookie sessions
- Profiles with avatar, bio, and links
- Follow and unfollow
- Posts with text and photos
- Comments with single-level replies
- Chronological feed of posts from followed users
- Notifications for new followers and reactions

### Strava integration

- OAuth2 connect and disconnect
- Automatic posts when an activity is completed (run, ride, swim, workout)
- Weekly activity summary on the profile

### Delivery

- Telegram Mini App: full-screen native-feeling shell, theme and viewport bridging, haptics, back-button handling
- Shared codebase for the desktop web experience
- Isolated production and development environments, each with its own Telegram bot

## Planned

### GitHub integration

- OAuth2 connect
- Automatic posts on new commit, merged pull request, and new repository
- Contribution statistics on the profile

### Token rewards (`$CRIT`)

`$CRIT` is the network's own token, earned automatically for real activity. Earning rules are configurable:

| Activity                  | Reward          |
|---------------------------|-----------------|
| Post per day              | 1 $CRIT         |
| GitHub commit synced      | 2 $CRIT         |
| Strava activity synced    | 3 $CRIT         |
| Seven-day activity streak | 10 $CRIT bonus  |
| New follower milestone    | 5 $CRIT         |

Spending:

- Custom profile badge
- Extended bio length
- Priority in follower suggestions
- Marketplace perks, later

Anti-abuse:

- Daily earning caps per user
- Rate limiting on sync endpoints
- Cooldown between same-source events

### On-chain layer

- EVM-compatible wallet connect
- A `$CRIT` token contract
- Claiming the in-app balance to an on-chain wallet
- Fast, low-cost transactions so balances move without friction
- Full token and transaction history

### Direct messages

Private one-to-one messaging between users. Targeted for summer, possibly sooner.

### Polish

- Real-time feed updates
- Dark and light themes
- Web push notifications
- A public API for third-party integrations

## Out of scope for v1

- Algorithmic feed ranking
- Advertising
- Content reporting and heavy moderation
- Groups and communities
