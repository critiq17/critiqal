package org.critiqal.domain.follow.service;

import org.critiqal.domain.follow.repository.FollowRepository;
import org.critiqal.domain.shared.exception.DomainException;
import org.critiqal.domain.user.User;
import org.critiqal.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FollowServiceImplTest {

    private final FollowRepository followRepo = mock(FollowRepository.class);
    private final UserService userService = mock(UserService.class);

    private FollowServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FollowServiceImpl(followRepo, userService);
    }

    @Test
    void followThrowsWhenTryingToFollowSelf() {
        var userId = uuid(1);
        var user = user(userId);
        when(userService.getById(userId)).thenReturn(user);

        assertThrows(DomainException.class, () -> service.follow(userId, userId));

        verify(followRepo, never()).save(any());
    }

    @Test
    void followReturnsWhenRelationshipAlreadyExists() {
        var followerId = uuid(1);
        var followingId = uuid(2);
        var follower = user(followerId);
        var following = user(followingId);
        when(userService.getById(followerId)).thenReturn(follower);
        when(userService.getById(followingId)).thenReturn(following);
        when(followRepo.isFollowing(followerId, followingId)).thenReturn(true);

        service.follow(followerId, followingId);

        verify(followRepo, never()).save(any());
    }

    @Test
    void followPersistsNewRelationship() {
        var followerId = uuid(1);
        var followingId = uuid(2);
        var follower = user(followerId);
        var following = user(followingId);
        when(userService.getById(followerId)).thenReturn(follower);
        when(userService.getById(followingId)).thenReturn(following);
        when(followRepo.isFollowing(followerId, followingId)).thenReturn(false);

        service.follow(followerId, followingId);

        verify(followRepo).save(argThat(follow ->
                follow.follower == follower && follow.following == following));
    }

    @Test
    void unfollowDelegatesToRepository() {
        var followerId = uuid(1);
        var followingId = uuid(2);

        service.unfollow(followerId, followingId);

        verify(followRepo).deleteByUsers(followerId, followingId);
    }

    @Test
    void getFollowersDelegatesToRepository() {
        var userId = uuid(3);
        var followers = List.of(user(uuid(1)), user(uuid(2)));
        when(followRepo.findFollowers(userId)).thenReturn(followers);

        assertSame(followers, service.getFollowers(userId));
    }

    @Test
    void getFollowingDelegatesToRepository() {
        var userId = uuid(3);
        var following = List.of(user(uuid(4)));
        when(followRepo.findFollowing(userId)).thenReturn(following);

        assertSame(following, service.getFollowing(userId));
    }

    @Test
    void isFollowingDelegatesToRepository() {
        var followerId = uuid(1);
        var followingId = uuid(2);
        var anotherId = uuid(3);
        when(followRepo.isFollowing(followerId, followingId)).thenReturn(true);

        assertTrue(service.isFollowing(followerId, followingId));
        assertFalse(service.isFollowing(followerId, anotherId));
    }

    @Test
    void getStatsReturnsRepositoryCounts() {
        var userId = uuid(8);
        when(followRepo.countFollowers(userId)).thenReturn(11L);
        when(followRepo.countFollowing(userId)).thenReturn(7L);

        var stats = service.getStats(userId);

        assertEquals(11L, stats.followers());
        assertEquals(7L, stats.following());
    }

    private static User user(UUID id) {
        var user = new User();
        user.id = id;
        return user;
    }

    private static UUID uuid(int value) {
        return UUID.fromString("00000000-0000-0000-0000-%012d".formatted(value));
    }
}
