package org.critiqal.domain.user;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {

    public Optional<User> findByUsername(String username) {
        return find("username", username).firstResultOptional();
    }

    public List<User> search(String query) {
        return find("LOWER(username) LIKE ?1 OR LOWER(name) LIKE ?1",
                "%" + query.toLowerCase() + "%")
                .list();
    }

    @Transactional
    public User createUser(String username, String passwordHash) {
        var user = new User();
        user.username = username;
        user.passwordHash = passwordHash;
        persist(user);
        return user;
    }
}
