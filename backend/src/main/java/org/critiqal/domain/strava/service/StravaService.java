package org.critiqal.domain.strava.service;

import org.critiqal.api.strava.response.StravaActivity;
import org.critiqal.api.strava.request.StravaConnection;
import java.util.List;
import java.util.Optional;

/**
 * Defines Strava integration operations for application users.
 * Handles connect, disconnect, and activity retrieval flows.
 */
public interface StravaService {
    public String getAuthorizationUrl(Long userId);
    public void handleCallback(Long userId, String code);
    public void disconnect(Long userId);
    public Optional<StravaConnection> getConnection(Long userId);
    public List<StravaActivity> getRecentActivities(Long userId, int limit);
}
