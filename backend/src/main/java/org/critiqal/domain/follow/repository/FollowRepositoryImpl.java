package org.critiqal.domain.follow.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.follow.Follow;
import org.critiqal.domain.user.User;

import java.util.List;

/**
 * Panache-backed implementation of {@link FollowRepository}.
 * Persists follow relationships and related lookup queries.
 */
@ApplicationScoped
public class FollowRepositoryImpl implements FollowRepository, PanacheRepository<Follow> {

    @Override
    public List<User> findFollowing(Long userId) {
        return find("follower.id = ?1", userId)
                .stream()
                .map(follow -> follow.following)
                .toList();
    }

    @Override
    public List<User> findFollowers(Long userId) {
        return find("following.id = ?1", userId)
                .stream()
                .map(follow -> follow.follower)
                .toList();
    }

    @Override
    public boolean isFollowing(Long followerId, Long followingId) {
        return find("follower.id = ?1 AND following.id = ?2", followerId, followingId)
                .firstResultOptional()
                .isPresent();
    }

    @Override
    public long countFollowers(Long userId) {
        return count("following.id = ?1", userId);
    }

    @Override
    public long countFollowing(Long userId) {
        return count("follower.id = ?1", userId);
    }

    @Override
    @Transactional
    public Follow save(Follow follow) {
        persist(follow);
        return follow;
    }

    @Override
    @Transactional
    public void deleteByUsers(Long followerId, Long followingId) {
        delete("follower.id = ?1 AND following.id = ?2", followerId, followingId);
    }
}
