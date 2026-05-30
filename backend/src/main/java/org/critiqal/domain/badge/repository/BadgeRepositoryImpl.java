package org.critiqal.domain.badge.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.critiqal.domain.badge.Badge;
import org.critiqal.domain.badge.BadgeCode;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class BadgeRepositoryImpl implements BadgeRepository, PanacheRepository<Badge> {

    @Override
    public Optional<Badge> findByCode(BadgeCode code) {
        return find("code", code.name()).firstResultOptional();
    }

    @Override
    public List<Badge> listAll() {
        return find("ORDER BY code ASC").list();
    }
}
