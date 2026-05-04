package org.critiqal.domain.follow;

import org.critiqal.domain.follow.repository.FollowRepository;
import org.critiqal.domain.follow.service.FollowService;
import org.critiqal.domain.follow.service.FollowServiceImpl;
import org.critiqal.domain.user.User;
import org.critiqal.domain.user.service.UserService;
import org.critiqal.domain.shared.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class FollowRepositoryTest {

    private final FollowRepository followRepo = mock(FollowRepository.class);
    private final UserService userService = mock(UserService.class);

    private final FollowService followService = new FollowServiceImpl(followRepo, userService);

    @Test
    void follow_self_throws() {
        var user = new User();
        user.id = 1L;

        when(userService.getById(1L)).thenReturn(user);

        assertThrows(DomainException.class,
                () -> followService.follow(1L, 1L));
    }

    @Test
    void follow_valid_callsRepo() {
        var follower = new User();
        follower.id = 1L;
        var following = new User();
        following.id = 2L;

        when(userService.getById(1L)).thenReturn(follower);
        when(userService.getById(2L)).thenReturn(following);

        followService.follow(1L, 2L);

        verify(followRepo).save(any(Follow.class));
    }

    @Test
    void unfollow_callsRepo() {
        followService.unfollow(1L, 2L);

        verify(followRepo).deleteByUsers(1L, 2L);
    }
}
