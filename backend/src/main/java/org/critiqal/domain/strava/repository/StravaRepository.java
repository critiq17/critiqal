package org.critiqal.domain.strava.repository;

import org.critiqal.domain.strava.StravaIntegration;

import java.util.Optional;

public interface StravaRepository {
    public Optional<StravaIntegration> findByUserId(Long userId);
    public boolean existsByUserId(Long userId);
}
