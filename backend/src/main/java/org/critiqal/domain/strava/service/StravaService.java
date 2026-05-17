package org.critiqal.domain.strava.service;

import org.critiqal.api.strava.response.StravaActivity;
import org.critiqal.api.strava.request.StravaConnection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Defines Strava integration operations for application users.
 * Handles connect, disconnect, and activity retrieval flows.
 */
public interface StravaService {
    public String getAuthorizationUrl(UUID userId);
    public void handleCallback(UUID userId, String code);
    public void disconnect(UUID userId);
    public Optional<StravaConnection> getConnection(UUID userId);
    public List<StravaActivity> getRecentActivities(UUID userId, int limit);
}
