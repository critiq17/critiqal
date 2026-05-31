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

    Optional<User> findByEmail(String email);

    List<User> search(String query);

    // Paged admin search. Blank query browses recent users.
    List<User> searchPaged(String query, int offset, int limit);

    long countSearch(String query);

    List<User> findRecent(int offset, int limit);

    long countAll();

    User save(User user);

    int clearExpiredPendingEmails();
}
