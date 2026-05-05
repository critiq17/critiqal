package org.critiqal.domain.user.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;
import org.critiqal.domain.shared.exception.ConflictException;
import org.critiqal.domain.shared.exception.NotFoundException;
import org.critiqal.domain.user.User;
import org.critiqal.domain.user.Username;
import org.critiqal.domain.user.event.UserRegisteredEvent;
import org.critiqal.domain.user.repository.UserRepository;
import org.critiqal.domain.auth.password.PasswordHash;

import java.util.List;

/**
 * Default implementation of {@link UserService}.
 * Coordinates registration, profile changes, and credential checks.
 */
@ApplicationScoped
public class UserServiceImpl implements UserService {

    private final UserRepository userRepo;
    private final PasswordHash passwordHash;
    private final Event<UserRegisteredEvent> userRegisteredEvent;

    public UserServiceImpl(UserRepository userRepo, PasswordHash passwordHash, Event<UserRegisteredEvent> userRegisteredEvent) {
        this.userRepo = userRepo;
        this.passwordHash = passwordHash;
        this.userRegisteredEvent = userRegisteredEvent;
    }

    @Override
    @Transactional
    public User register(Username username, String password) {
        if (userRepo.findByUsername(username).isPresent()) {
            throw new ConflictException("Username already taken");
        }

        var user = new User();
        user.username = username.value();
        user.passwordHash = passwordHash.hash(password);

        user = userRepo.save(user);
        userRegisteredEvent.fireAsync(new UserRegisteredEvent(user.id, user.username));
        return user;
    }

    @Override
    public User getByUsername(Username username) {
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Override
    public User getById(Long id) {
        return userRepo.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Override
    public List<User> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return userRepo.search(query);
    }

    @Override
    @Transactional
    public User updateProfile(Long userId, String name, String bio) {
        var user = getById(userId);
        user.name = name;
        user.bio = bio;
        return user;
    }

    @Override
    @Transactional
    public void updateAvatar(Long userId, String avatarUrl) {
        var user = getById(userId);
        user.avatarUrl = avatarUrl;
    }

    @Override
    public boolean checkPassword(Username username, String rawPassword) {
        return userRepo.findByUsername(username)
                .map(user -> passwordHash.verify(rawPassword, user.passwordHash))
                .orElse(false);
    }
}
