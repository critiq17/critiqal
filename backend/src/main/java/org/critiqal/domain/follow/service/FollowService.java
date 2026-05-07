package org.critiqal.domain.follow.service;

import org.critiqal.domain.user.User;

import java.util.List;
import java.util.UUID;

/**
 * Defines follow management operations between users.
 * Handles follow state changes, lookups, and statistics.
 */
public interface FollowService {

    void follow(UUID followerId, UUID followingId);

    void unfollow(UUID followerId, UUID followingId);

    List<User> getFollowers(UUID userId);

    List<User> getFollowing(UUID userId);

    boolean isFollowing(UUID followerId, UUID followingId);

    FollowStats getStats(UUID userId);

    record FollowStats(long followers, long following) {}
}
