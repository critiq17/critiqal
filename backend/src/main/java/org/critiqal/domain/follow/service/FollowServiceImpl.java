package org.critiqal.domain.follow.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.follow.Follow;
import org.critiqal.domain.follow.repository.FollowRepository;
import org.critiqal.domain.shared.exception.DomainException;
import org.critiqal.domain.user.User;
import org.critiqal.domain.user.service.UserService;

import java.util.List;
import java.util.UUID;

/**
 * Default implementation of {@link FollowService}.
 * Applies follow domain rules and computes follow statistics.
 */
@ApplicationScoped
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepo;
    private final UserService userService;

    public FollowServiceImpl(FollowRepository followRepo,
                             UserService userService) {
        this.followRepo = followRepo;
        this.userService = userService;
    }

    @Override
    @Transactional
    public void follow(UUID followerId, UUID followingId) {
        var follower = userService.getById(followerId);
        var following = userService.getById(followingId);

        if (follower.id.equals(following.id)) {
            throw new DomainException("Cannot follow yourself");
        }
        if (followRepo.isFollowing(follower.id, following.id)) {
            return;
        }

        var follow = new Follow();
        follow.follower = follower;
        follow.following = following;
        followRepo.save(follow);
    }

    @Override
    @Transactional
    public void unfollow(UUID followerId, UUID followingId) {
        followRepo.deleteByUsers(followerId, followingId);
    }

    @Override
    public List<User> getFollowers(UUID userId) {
        return followRepo.findFollowers(userId);
    }

    @Override
    public List<User> getFollowing(UUID userId) {
        return followRepo.findFollowing(userId);
    }

    @Override
    public boolean isFollowing(UUID followerId, UUID followingId) {
        return followRepo.isFollowing(followerId, followingId);
    }

    @Override
    public FollowService.FollowStats getStats(UUID userId) {
        return new FollowService.FollowStats(
                followRepo.countFollowers(userId),
                followRepo.countFollowing(userId)
        );
    }
}
