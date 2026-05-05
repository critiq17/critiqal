package org.critiqal.domain.strava.repository;

import org.critiqal.domain.strava.StravaIntegration;

import java.util.Optional;

/**
 * Defines persistence operations for Strava integrations.
 * Supports lookup, existence checks, and lifecycle changes per user.
 */
public interface StravaRepository {
    Optional<StravaIntegration> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    StravaIntegration save(StravaIntegration integration);

    void delete(StravaIntegration integration);
}
