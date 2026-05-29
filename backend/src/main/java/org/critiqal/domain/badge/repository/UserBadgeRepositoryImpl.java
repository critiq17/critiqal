package org.critiqal.domain.badge.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.badge.UserBadge;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class UserBadgeRepositoryImpl implements UserBadgeRepository, PanacheRepository<UserBadge> {

    @Override
    public boolean existsByUserIdAndBadgeCode(UUID userId, String code) {
        return count("user.id = ?1 AND badge.code = ?2", userId, code) > 0;
    }

    @Override
    public List<UserBadge> findByUserId(UUID userId) {
        return find("user.id = ?1 ORDER BY awardedAt ASC", userId).list();
    }

    @Override
    public long countByBadgeCode(String code) {
        return count("badge.code = ?1", code);
    }

    @Override
    @Transactional
    public UserBadge save(UserBadge userBadge) {
        persist(userBadge);
        return userBadge;
    }
}
