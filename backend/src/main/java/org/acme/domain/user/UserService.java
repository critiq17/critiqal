package org.acme.domain.user;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.utils.PasswordHash;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.List;

@ApplicationScoped
public class UserService {

    @Inject UserRepository userRepo;
    @Inject PasswordHash passwordHash;

    @Transactional
    public User register(String username, String password) {
        if (userRepo.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }
        return userRepo.createUser(username, passwordHash.hash(password));
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
    public User updateProfile(Long userId, String name, String bio, String avatarUrl) {
        var user = getById(userId);
        user.name = name;
        user.bio = bio;
        user.avatarUrl = avatarUrl;
        return user;
    }

    @Transactional
    public void updateAvatar(Long userId, String avatarUrl) {
        var user = getById(userId);
        user.avatarUrl = avatarUrl;
    }

    public boolean checkPassword(String username, String rawPassword) {
        var user = getByUsername(username);
        return passwordHash.verify(rawPassword, user.passwordHash);
    }
}
