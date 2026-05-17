package org.critiqal.api.user.response;

// Lightweight aggregate of public counts shown on the profile screen.
// Kept separate from UserDTO to avoid N+1 counts on user lists/feeds.
public record UserStatsDTO(
        long postsCount,
        long followersCount,
        long followingCount
) {}
