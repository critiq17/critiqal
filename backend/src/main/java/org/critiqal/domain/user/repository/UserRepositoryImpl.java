package org.critiqal.domain.user.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.user.User;
import org.critiqal.domain.user.Username;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Panache-backed implementation of {@link UserRepository}.
 * Persists users and executes lookup or search queries.
 */
@ApplicationScoped
public class UserRepositoryImpl implements UserRepository, PanacheRepository<User> {

    @Override
    public Optional<User> findByIdOptional(UUID id) {
        return find("id", id).firstResultOptional();
    }

    @Override
    public Optional<User> findByUsername(Username username) {
        return find("username", username.value()).firstResultOptional();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return find("LOWER(email) = ?1", email.toLowerCase()).firstResultOptional();
    }

    @Override
    public List<User> search(String query) {
        return find("LOWER(username) LIKE ?1 OR LOWER(name) LIKE ?1",
                "%" + query.toLowerCase() + "%")
                .list();
    }

    @Override
    public User save(User user) {
        persist(user);
        return user;
    }

    @Override
    @Transactional
    public int clearExpiredPendingEmails() {
        return update("""
                pendingEmail = null
                WHERE pendingEmail IS NOT NULL
                AND id NOT IN (
                    SELECT vt.user.id FROM VerificationToken vt
                    WHERE vt.type = 'EMAIL_VERIFY'
                    AND vt.usedAt IS NULL
                    AND vt.expiresAt > ?1
                )
                """, Instant.now());
    }
}
