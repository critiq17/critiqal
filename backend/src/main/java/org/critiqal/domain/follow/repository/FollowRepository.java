package org.critiqal.domain.follow.repository;

import org.critiqal.domain.follow.Follow;
import org.critiqal.domain.user.User;

import java.util.List;

public interface FollowRepository {

    List<User> findFollowing(Long userId);

    List<User> findFollowers(Long userId);

    boolean isFollowing(Long followerId, Long followingId);

    long countFollowers(Long userId);

    long countFollowing(Long userId);

    Follow save(Follow follow);

    void deleteByUsers(Long followerId, Long followingId);
}
