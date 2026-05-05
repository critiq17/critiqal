package org.critiqal.domain.user.service;

import org.critiqal.domain.user.User;
import org.critiqal.domain.user.Username;

import java.util.List;

/**
 * Defines user management operations.
 * Handles registration, lookup, profile updates, and password checks.
 */
public interface UserService {

    User register(Username username, String password);

    User getByUsername(Username username);

    User getById(Long id);

    List<User> search(String query);

    User updateProfile(Long userId, String name, String bio);

    void updateAvatar(Long userId, String avatarUrl);

    boolean checkPassword(Username username, String rawPassword);
}
