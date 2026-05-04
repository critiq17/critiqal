package org.critiqal.domain.user.repository;

import org.critiqal.domain.user.User;
import org.critiqal.domain.user.Username;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findByIdOptional(Long id);

    Optional<User> findByUsername(Username username);

    List<User> search(String query);

    User save(User user);
}
