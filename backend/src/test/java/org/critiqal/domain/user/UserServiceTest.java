package org.critiqal.domain.user;

import jakarta.enterprise.event.Event;
import org.critiqal.domain.shared.exception.ConflictException;
import org.critiqal.domain.shared.exception.NotFoundException;
import org.critiqal.domain.user.event.UserRegisteredEvent;
import org.critiqal.domain.user.repository.UserRepository;
import org.critiqal.domain.user.service.UserService;
import org.critiqal.domain.user.service.UserServiceImpl;
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
        userService = new UserServiceImpl(userRepo, passwordHash, registeredEvent);
    }

    @Test
    void register_duplicateUsername_throws() {
        var existing = new User();
        when(userRepo.findByUsername(Username.of("taken"))).thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class,
                () -> userService.register(Username.of("taken"), "pass123"));
    }

    @Test
    void register_valid_callsRepo() {
        when(userRepo.findByUsername(Username.of("newuser"))).thenReturn(Optional.empty());
        when(passwordHash.hash("pass123")).thenReturn("hashed");
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = userService.register(Username.of("newuser"), "pass123");

        assertEquals("newuser", result.username);
        assertEquals("hashed", result.passwordHash);
        verify(userRepo).save(argThat(user ->
                "newuser".equals(user.username) && "hashed".equals(user.passwordHash)));
    }

    @Test
    void getById_notFound_throws() {
        when(userRepo.findByIdOptional(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
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
        when(userRepo.findByUsername(Username.of("alice"))).thenReturn(Optional.of(user));
        when(passwordHash.verify("pass123", "hashed")).thenReturn(true);

        assertTrue(userService.checkPassword(Username.of("alice"), "pass123"));
    }

    @Test
    void checkPassword_wrongPassword_returnsFalse() {
        var user = new User();
        user.passwordHash = "hashed";
        when(userRepo.findByUsername(Username.of("alice"))).thenReturn(Optional.of(user));
        when(passwordHash.verify("wrong", "hashed")).thenReturn(false);

        assertFalse(userService.checkPassword(Username.of("alice"), "wrong"));
    }
}
