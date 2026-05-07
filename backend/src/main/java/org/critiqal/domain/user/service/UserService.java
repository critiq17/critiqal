package org.critiqal.domain.user.service;

import org.critiqal.domain.user.User;
import org.critiqal.domain.user.Username;

import java.util.List;
import java.util.UUID;

/**
 * Defines user management operations.
 * Handles registration, lookup, profile updates, and password checks.
 */
public interface UserService {

    User register(Username username, String password);

    User getByUsername(Username username);

    User getById(UUID id);

    List<User> search(String query);

    User updateProfile(UUID userId, String name, String bio);

    void updateAvatar(UUID userId, String avatarUrl);

    boolean checkPassword(Username username, String rawPassword);
}
