package org.critiqal.domain.strava.service;

import org.critiqal.api.dtos.strava.StravaActivityDTO;
import org.critiqal.api.dtos.strava.StravaConnectionDTO;
import java.util.List;
import java.util.Optional;

public interface StravaService {
    public String getAuthorizationUrl(Long userId);
    public void handleCallback(Long userId, String code);
    public void disconnect(Long userId);
    public Optional<StravaConnectionDTO> getConnection(Long userId);
    public List<StravaActivityDTO> getRecentActivities(Long userId, int limit);
}
