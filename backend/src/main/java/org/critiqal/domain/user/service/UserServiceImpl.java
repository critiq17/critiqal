package org.critiqal.domain.user.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;
import org.critiqal.domain.activity.ActivityEvent;
import org.critiqal.domain.activity.repository.UserActivityStatsRepository;
import org.critiqal.domain.shared.exception.ConflictException;
import org.critiqal.domain.shared.exception.DomainException;
import org.critiqal.domain.shared.exception.NotFoundException;
import org.critiqal.domain.user.User;
import org.critiqal.domain.user.Username;
import org.critiqal.domain.user.event.UserRegisteredEvent;
import org.critiqal.domain.user.repository.UserRepository;
import org.critiqal.domain.auth.password.PasswordHash;

import java.util.List;
import java.util.UUID;

/**
 * Default implementation of {@link UserService}.
 * Coordinates registration, profile changes, and credential checks.
 */
@ApplicationScoped
public class UserServiceImpl implements UserService {

    private final UserRepository userRepo;
    private final PasswordHash passwordHash;
    private final Event<UserRegisteredEvent> userRegisteredEvent;
    private final UserActivityStatsRepository statsRepo;

    public UserServiceImpl(UserRepository userRepo, PasswordHash passwordHash,
                           Event<UserRegisteredEvent> userRegisteredEvent, UserActivityStatsRepository statsRepo) {
        this.userRepo = userRepo;
        this.passwordHash = passwordHash;
        this.userRegisteredEvent = userRegisteredEvent;
        this.statsRepo = statsRepo;
    }

    @Override
    @Transactional
    public User register(Username username, String password) {
        validatePassword(password);
        if (userRepo.findByUsername(username).isPresent()) {
            throw new ConflictException("Username already taken");
        }

        var user = new User();
        user.username = username.value();
        user.passwordHash = passwordHash.hash(password);

        user = userRepo.save(user);
        statsRepo.findOrCreate(user.id);
        userRegisteredEvent.fireAsync(new UserRegisteredEvent(user.id, user.username));
        return user;
    }

    @Override
    public User getByUsername(Username username) {
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Override
    public User getById(UUID id) {
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
    public User updateProfile(UUID userId, String name, String bio) {
        var user = getById(userId);
        user.name = name;
        user.bio = bio;
        return user;
    }

    @Override
    @Transactional
    public void updateAvatar(UUID userId, String avatarUrl) {
        var user = getById(userId);
        user.avatarUrl = avatarUrl;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new DomainException("Password must be at least 8 characters");
        }
    }

    @Override
    public boolean checkPassword(Username username, String rawPassword) {
        return userRepo.findByUsername(username)
                .map(user -> passwordHash.verify(rawPassword, user.passwordHash))
                .orElse(false);
    }
}
