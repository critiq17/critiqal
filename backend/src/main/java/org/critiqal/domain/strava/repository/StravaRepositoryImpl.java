package org.critiqal.domain.strava.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.critiqal.domain.strava.StravaIntegration;

import java.util.Optional;

/**
 * Panache-backed implementation of {@link StravaRepository}.
 * Persists Strava integration records for users.
 */
@ApplicationScoped
public class StravaRepositoryImpl implements StravaRepository, PanacheRepository<StravaIntegration> {

    @Override
    public Optional<StravaIntegration> findByUserId(Long userId) {
        return find("user.id", userId).firstResultOptional();
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return count("user.id", userId) > 0;
    }

    @Override
    public StravaIntegration save(StravaIntegration integration) {
        persist(integration);
        return integration;
    }

    @Override
    public void delete(StravaIntegration integration) {
        delete("id", integration.id);
    }
}
