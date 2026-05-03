package org.critiqal.domain.follow;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.critiqal.domain.user.User;
import org.critiqal.domain.user.UserService;

import java.util.List;

@ApplicationScoped
public class FollowService {

    private final FollowRepository followRepo;
    private final UserService userService;

    public FollowService(FollowRepository followRepo,
                         UserService userService) {
        this.followRepo = followRepo;
        this.userService = userService;
    }

    @Transactional
    public void follow(Long followerId, Long followingId) {
        var follower = userService.getById(followerId);
        var following = userService.getById(followingId);
        followRepo.follow(follower, following);
    }

    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        followRepo.unfollow(followerId, followingId);
    }

    public List<User> getFollowers(Long userId) {
        return followRepo.findFollowers(userId);
    }

    public List<User> getFollowing(Long userId) {
        return followRepo.findFollowing(userId);
    }

    public boolean isFollowing(Long followerId, Long followingId) {
        return followRepo.isFollowing(followerId, followingId);
    }

    public record FollowStats(long followers, long following) {}

    public FollowStats getStats(Long userId) {
        return new FollowStats(
                followRepo.countFollowers(userId),
                followRepo.countFollowing(userId)
        );
    }
}
