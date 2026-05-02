package org.critiqal.infra.postgres;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.critiqal.domain.strava.StravaIntegration;

import java.util.Optional;

/*
    StravaRepository
 */
@ApplicationScoped
public class StravaRepository implements PanacheRepository<StravaIntegration> {

    public Optional<StravaIntegration> findByUserId(Long userId) {
        return find("user.id", userId).firstResultOptional();
    }

    public boolean existsByUserId(Long userId) {
        return count("user.id", userId) > 0;
    }
}
