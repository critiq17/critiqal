package org.critiqal.domain.strava.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.critiqal.domain.strava.StravaIntegration;

import java.util.Optional;

/*
    StravaRepository
 */
@ApplicationScoped
public class StravaRepositoryImpl implements StravaRepository, PanacheRepository<StravaIntegration> {

    public Optional<StravaIntegration> findByUserId(Long userId) {
        return find("user.id", userId).firstResultOptional();
    }

    public boolean existsByUserId(Long userId) {
        return count("user.id", userId) > 0;
    }
}
