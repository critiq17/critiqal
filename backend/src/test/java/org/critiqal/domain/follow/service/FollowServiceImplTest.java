package org.critiqal.domain.follow.service;

import org.critiqal.domain.follow.Follow;
import org.critiqal.domain.follow.repository.FollowRepository;
import org.critiqal.domain.shared.exception.DomainException;
import org.critiqal.domain.user.User;
import org.critiqal.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        var user = user(1L);
        when(userService.getById(1L)).thenReturn(user);

        assertThrows(DomainException.class, () -> service.follow(1L, 1L));

        verify(followRepo, never()).save(any());
    }

    @Test
    void followReturnsWhenRelationshipAlreadyExists() {
        var follower = user(1L);
        var following = user(2L);
        when(userService.getById(1L)).thenReturn(follower);
        when(userService.getById(2L)).thenReturn(following);
        when(followRepo.isFollowing(1L, 2L)).thenReturn(true);

        service.follow(1L, 2L);

        verify(followRepo, never()).save(any());
    }

    @Test
    void followPersistsNewRelationship() {
        var follower = user(1L);
        var following = user(2L);
        when(userService.getById(1L)).thenReturn(follower);
        when(userService.getById(2L)).thenReturn(following);
        when(followRepo.isFollowing(1L, 2L)).thenReturn(false);

        service.follow(1L, 2L);

        verify(followRepo).save(argThat(follow ->
                follow.follower == follower && follow.following == following));
    }

    @Test
    void unfollowDelegatesToRepository() {
        service.unfollow(1L, 2L);

        verify(followRepo).deleteByUsers(1L, 2L);
    }

    @Test
    void getFollowersDelegatesToRepository() {
        var followers = List.of(user(1L), user(2L));
        when(followRepo.findFollowers(3L)).thenReturn(followers);

        assertSame(followers, service.getFollowers(3L));
    }

    @Test
    void getFollowingDelegatesToRepository() {
        var following = List.of(user(4L));
        when(followRepo.findFollowing(3L)).thenReturn(following);

        assertSame(following, service.getFollowing(3L));
    }

    @Test
    void isFollowingDelegatesToRepository() {
        when(followRepo.isFollowing(1L, 2L)).thenReturn(true);

        assertTrue(service.isFollowing(1L, 2L));
        assertFalse(service.isFollowing(1L, 3L));
    }

    @Test
    void getStatsReturnsRepositoryCounts() {
        when(followRepo.countFollowers(8L)).thenReturn(11L);
        when(followRepo.countFollowing(8L)).thenReturn(7L);

        var stats = service.getStats(8L);

        assertEquals(11L, stats.followers());
        assertEquals(7L, stats.following());
    }

    private static User user(Long id) {
        var user = new User();
        user.id = id;
        return user;
    }
}
