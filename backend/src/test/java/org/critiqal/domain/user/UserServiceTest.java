package org.critiqal.domain.user;

import jakarta.enterprise.event.Event;
import org.critiqal.utils.PasswordHash;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private final UserRepository userRepo = mock(UserRepository.class);
    private final PasswordHash passwordHash = mock(PasswordHash.class);
    @SuppressWarnings("unchecked")
    private final Event<UserRegisteredEvent> registeredEvent = mock(Event.class);

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepo, passwordHash, registeredEvent);
    }

    @Test
    void register_duplicateUsername_throws() {
        var existing = new User();
        when(userRepo.findByUsername("taken")).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class,
                () -> userService.register("taken", "pass123"));
    }

    @Test
    void register_valid_callsRepo() {
        when(userRepo.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordHash.hash("pass123")).thenReturn("hashed");
        var created = new User();
        when(userRepo.createUser("newuser", "hashed")).thenReturn(created);

        var result = userService.register("newuser", "pass123");

        assertSame(created, result);
        verify(userRepo).createUser("newuser", "hashed");
    }

    @Test
    void getById_notFound_throws() {
        when(userRepo.findByIdOptional(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> userService.getById(99L));
    }

    @Test
    void search_blankQuery_returnsEmpty() {
        var result = userService.search("  ");

        assertTrue(result.isEmpty());
        verifyNoInteractions(userRepo);
    }

    @Test
    void checkPassword_correctPassword_returnsTrue() {
        var user = new User();
        user.passwordHash = "hashed";
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordHash.verify("pass123", "hashed")).thenReturn(true);

        assertTrue(userService.checkPassword("alice", "pass123"));
    }

    @Test
    void checkPassword_wrongPassword_returnsFalse() {
        var user = new User();
        user.passwordHash = "hashed";
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordHash.verify("wrong", "hashed")).thenReturn(false);

        assertFalse(userService.checkPassword("alice", "wrong"));
    }
}
