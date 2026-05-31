package org.critiqal.domain.ban.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.ban.UserBan;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserBanRepositoryImpl implements UserBanRepository, PanacheRepository<UserBan> {

    @Override
    public Optional<UserBan> findActiveByUserId(UUID userId) {
        return find("user.id = ?1 AND liftedAt IS NULL", userId).firstResultOptional();
    }

    @Override
    @Transactional
    public UserBan save(UserBan ban) {
        persist(ban);
        return ban;
    }
}
