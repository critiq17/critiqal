package org.critiqal.domain.user.repository;

import org.critiqal.domain.user.User;
import org.critiqal.domain.user.Username;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Defines persistence operations for users.
 * Supports lookup, search, and storage.
 */
public interface UserRepository {

    Optional<User> findByIdOptional(UUID id);

    Optional<User> findByUsername(Username username);

    List<User> search(String query);

    User save(User user);
}
