package org.critiqal.domain.follow.repository;

import org.critiqal.domain.follow.Follow;
import org.critiqal.domain.user.User;

import java.util.List;
import java.util.UUID;

/**
 * Defines persistence operations for follow relationships.
 * Supports follower lookups, counts, and follow lifecycle changes.
 */
public interface FollowRepository {

    List<User> findFollowing(UUID userId);

    List<User> findFollowers(UUID userId);

    boolean isFollowing(UUID followerId, UUID followingId);

    long countFollowers(UUID userId);

    long countFollowing(UUID userId);

    Follow save(Follow follow);

    void deleteByUsers(UUID followerId, UUID followingId);
}
