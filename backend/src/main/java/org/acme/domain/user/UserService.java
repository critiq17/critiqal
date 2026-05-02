package org.acme.domain.user;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.utils.PasswordHash;

import java.util.List;

@ApplicationScoped
public class UserService {

    private final UserRepository userRepo;
    private final PasswordHash passwordHash;
    private final Event<UserRegisteredEvent> userRegisteredEvent;

    @Inject
    public UserService(UserRepository userRepo, PasswordHash passwordHash, Event<UserRegisteredEvent> userRegisteredEvent) {
        this.userRepo = userRepo;
        this.passwordHash = passwordHash;
        this.userRegisteredEvent = userRegisteredEvent;
    }

    @Transactional
    public User register(String username, String password) {
        if (userRepo.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }
        var user = userRepo.createUser(username, passwordHash.hash(password));
        userRegisteredEvent.fireAsync(new UserRegisteredEvent(user.id, user.username));
        return user;
    }

    public User getByUsername(String username) {
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public User getById(Long id) {
        return userRepo.findByIdOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public List<User> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return userRepo.search(query);
    }

    @Transactional
    public User updateProfile(Long userId, String name, String bio) {
        var user = getById(userId);
        user.name = name;
        user.bio = bio;
        return user;
    }

    @Transactional
    public void updateAvatar(Long userId, String avatarUrl) {
        var user = getById(userId);
        user.avatarUrl = avatarUrl;
    }

    public boolean checkPassword(String username, String rawPassword) {
        return userRepo.findByUsername(username)
                .map(user -> passwordHash.verify(rawPassword, user.passwordHash))
                .orElse(false);
    }
}
