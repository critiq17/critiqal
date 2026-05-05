package org.critiqal.domain.user.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.critiqal.domain.user.User;
import org.critiqal.domain.user.Username;

import java.util.List;
import java.util.Optional;

/**
 * Panache-backed implementation of {@link UserRepository}.
 * Persists users and executes lookup or search queries.
 */
@ApplicationScoped
public class UserRepositoryImpl implements UserRepository, PanacheRepository<User> {

    @Override
    public Optional<User> findByIdOptional(Long id) {
        return find("id", id).firstResultOptional();
    }

    @Override
    public Optional<User> findByUsername(Username username) {
        return find("username", username.value()).firstResultOptional();
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
}
