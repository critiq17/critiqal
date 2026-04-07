package org.acme.domain.follow;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.acme.domain.user.User;

import java.util.List;

@ApplicationScoped
public class FollowRepository implements PanacheRepository<Follow> {

    public List<User> findFollowing(Long userId) {
        return find("follower.id = ?1", userId)
                .stream()
                .map(f -> f.following)
                .toList();
    }

    public List<User> findFollowers(Long userId) {
        return find("following.id = ?1", userId)
                .stream()
                .map(f -> f.follower)
                .toList();
    }

    public boolean isFollowing(Long followerId, Long followingId) {
        return find("follower.id = ?1 AND following.id = ?2", followerId, followingId)
                .firstResultOptional()
                .isPresent();
    }

    public long countFollowers(Long userId) {
        return count("following.id = ?1", userId);
    }

    public long countFollowing(Long userId) {
        return count("follower.id = ?1", userId);
    }

    @Transactional
    public void follow(User follower, User following) {
        if (follower.id.equals(following.id)) {
            throw new IllegalArgumentException("Cannot follow yourself");
        }
        if (isFollowing(follower.id, following.id)) {
            return;
        }

        var follow = new Follow();
        follow.follower = follower;
        follow.following = following;
        persist(follow);
    }

    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        delete("follower.id = ?1 AND following.id = ?2", followerId, followingId);
    }
}
