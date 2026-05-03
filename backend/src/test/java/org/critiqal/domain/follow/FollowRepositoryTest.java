package org.critiqal.domain.follow;

import org.critiqal.domain.user.User;
import org.critiqal.domain.user.UserService;
import org.critiqal.domain.shared.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class FollowRepositoryTest {

    private final FollowRepository followRepo = mock(FollowRepository.class);
    private final UserService userService = mock(UserService.class);

    private FollowService followService;

    @BeforeEach
    void setUp() {
        followService = new FollowService();
        injectField(followService, "followRepo", followRepo);
        injectField(followService, "userService", userService);
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void follow_self_throws() {
        var user = new User();
        user.id = 1L;

        when(userService.getById(1L)).thenReturn(user);
        doThrow(new DomainException("Cannot follow yourself"))
                .when(followRepo).follow(user, user);

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

        verify(followRepo).follow(follower, following);
    }

    @Test
    void unfollow_callsRepo() {
        followService.unfollow(1L, 2L);

        verify(followRepo).unfollow(1L, 2L);
    }
}
