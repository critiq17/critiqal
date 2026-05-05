package org.critiqal.domain.follow.service;

import org.critiqal.domain.user.User;

import java.util.List;

/**
 * Defines follow management operations between users.
 * Handles follow state changes, lookups, and statistics.
 */
public interface FollowService {

    void follow(Long followerId, Long followingId);

    void unfollow(Long followerId, Long followingId);

    List<User> getFollowers(Long userId);

    List<User> getFollowing(Long userId);

    boolean isFollowing(Long followerId, Long followingId);

    FollowStats getStats(Long userId);

    record FollowStats(long followers, long following) {}
}
