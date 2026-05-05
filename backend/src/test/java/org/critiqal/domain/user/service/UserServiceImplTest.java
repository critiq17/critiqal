package org.critiqal.domain.user.service;

import jakarta.enterprise.event.Event;
import org.critiqal.domain.auth.password.PasswordHash;
import org.critiqal.domain.shared.exception.ConflictException;
import org.critiqal.domain.shared.exception.NotFoundException;
import org.critiqal.domain.user.User;
import org.critiqal.domain.user.Username;
import org.critiqal.domain.user.event.UserRegisteredEvent;
import org.critiqal.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    private final UserRepository userRepo = mock(UserRepository.class);
    private final PasswordHash passwordHash = mock(PasswordHash.class);
    @SuppressWarnings("unchecked")
    private final Event<UserRegisteredEvent> registeredEvent = mock(Event.class);

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(userRepo, passwordHash, registeredEvent);
    }

    @Test
    void registerThrowsWhenUsernameAlreadyTaken() {
        var username = Username.of("taken_user");
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(user(1L, username.value())));

        assertThrows(ConflictException.class, () -> service.register(username, "secret"));

        verify(passwordHash, never()).hash(any());
        verify(userRepo, never()).save(any());
        verifyNoInteractions(registeredEvent);
    }

    @Test
    void registerHashesPasswordPersistsUserAndFiresEvent() {
        var username = Username.of("new_user");
        when(userRepo.findByUsername(username)).thenReturn(Optional.empty());
        when(passwordHash.hash("secret")).thenReturn("hashed-secret");
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> {
            var saved = invocation.<User>getArgument(0);
            saved.id = 42L;
            return saved;
        });

        var saved = service.register(username, "secret");

        assertEquals(42L, saved.id);
        assertEquals("new_user", saved.username);
        assertEquals("hashed-secret", saved.passwordHash);
        verify(userRepo).save(argThat(user ->
                "new_user".equals(user.username) && "hashed-secret".equals(user.passwordHash)));
        verify(registeredEvent).fireAsync(argThat(event ->
                event.userId().equals(42L) && event.username().equals("new_user")));
    }

    @Test
    void getByUsernameReturnsRepositoryResult() {
        var username = Username.of("alice_1");
        var user = user(7L, username.value());
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(user));

        assertSame(user, service.getByUsername(username));
    }

    @Test
    void getByUsernameThrowsWhenMissing() {
        var username = Username.of("missing_user");
        when(userRepo.findByUsername(username)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getByUsername(username));
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(userRepo.findByIdOptional(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getById(99L));
    }

    @Test
    void searchReturnsEmptyForNullOrBlankQuery() {
        assertTrue(service.search(null).isEmpty());
        assertTrue(service.search("   ").isEmpty());

        verifyNoInteractions(userRepo);
    }

    @Test
    void searchDelegatesForNonBlankQuery() {
        var expected = List.of(user(1L, "alice"));
        when(userRepo.search("alice")).thenReturn(expected);

        assertSame(expected, service.search("alice"));
    }

    @Test
    void updateProfileMutatesLoadedUser() {
        var user = user(5L, "profile_user");
        when(userRepo.findByIdOptional(5L)).thenReturn(Optional.of(user));

        var updated = service.updateProfile(5L, "Alice", "Runner");

        assertSame(user, updated);
        assertEquals("Alice", user.name);
        assertEquals("Runner", user.bio);
    }

    @Test
    void updateAvatarMutatesLoadedUser() {
        var user = user(5L, "avatar_user");
        when(userRepo.findByIdOptional(5L)).thenReturn(Optional.of(user));

        service.updateAvatar(5L, "https://cdn/avatar.jpg");

        assertEquals("https://cdn/avatar.jpg", user.avatarUrl);
    }

    @Test
    void checkPasswordReturnsTrueWhenHashMatches() {
        var username = Username.of("alice_2");
        var user = user(2L, username.value());
        user.passwordHash = "hashed";
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordHash.verify("secret", "hashed")).thenReturn(true);

        assertTrue(service.checkPassword(username, "secret"));
    }

    @Test
    void checkPasswordReturnsFalseWhenUserMissing() {
        var username = Username.of("ghost_1");
        when(userRepo.findByUsername(username)).thenReturn(Optional.empty());

        assertFalse(service.checkPassword(username, "secret"));
        verify(passwordHash, never()).verify(any(), any());
    }

    private static User user(Long id, String username) {
        var user = new User();
        user.id = id;
        user.username = username;
        return user;
    }
}
