package org.critiqal.domain.strava.repository;

import org.critiqal.domain.strava.StravaIntegration;

import java.util.Optional;

public interface StravaRepository {
    Optional<StravaIntegration> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    StravaIntegration save(StravaIntegration integration);

    void delete(StravaIntegration integration);
}
