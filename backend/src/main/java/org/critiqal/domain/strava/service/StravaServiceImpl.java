package org.critiqal.domain.strava.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.api.strava.response.StravaActivity;
import org.critiqal.api.strava.request.StravaConnection;
import org.critiqal.domain.shared.exception.NotFoundException;
import org.critiqal.domain.strava.StravaIntegration;
import org.critiqal.domain.user.service.UserService;
import org.critiqal.domain.strava.gateway.Strava0AuthClient;
import org.critiqal.domain.strava.repository.StravaRepository;
import org.critiqal.infra.strava.StravaApiClient;
import org.critiqal.infra.strava.StravaTokenRefresher;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link StravaService}.
 * Coordinates OAuth state, token refresh, and activity retrieval.
 */
@ApplicationScoped
public class StravaServiceImpl implements StravaService {

    private final StravaRepository stravaRepo;
    private final Strava0AuthClient oAuthClient;
    private final StravaApiClient apiClient;
    private final StravaTokenRefresher tokenRefresher;
    private final UserService userService;

    public StravaServiceImpl(StravaRepository stravaRepo,
                             Strava0AuthClient oAuthClient,
                             StravaApiClient apiClient,
                             StravaTokenRefresher tokenRefresher,
                             UserService userService) {
        this.stravaRepo = stravaRepo;
        this.oAuthClient = oAuthClient;
        this.apiClient = apiClient;
        this.tokenRefresher = tokenRefresher;
        this.userService = userService;
    }

    public String getAuthorizationUrl(Long userId) {
        return oAuthClient.buildAuthorizationUrl(userId.toString());
    }

    @Transactional
    public void handleCallback(Long userId, String code) {
        var tokenResponse = oAuthClient.exchangeCode(code);
        var athlete = tokenResponse.athlete();
        var user = userService.getById(userId);

        var integration = stravaRepo.findByUserId(userId)
                .orElseGet(StravaIntegration::new);

        integration.user = user;
        integration.accessToken = tokenResponse.accessToken();
        integration.refreshToken = tokenResponse.refreshToken();
        integration.expiresAt = tokenResponse.expiresAt();
        integration.athleteId = athlete.id();
        integration.athleteUsername = athlete.username();
        integration.athleteFirstname = athlete.firstName();
        integration.athleteLastname = athlete.lastName();
        integration.athleteCity = athlete.city();
        integration.athleteAvatarUrl = athlete.profileUrl();

        stravaRepo.save(integration);
    }

    @Transactional
    public void disconnect(Long userId) {
        stravaRepo.findByUserId(userId)
                .ifPresent(stravaRepo::delete);
    }

    public Optional<StravaConnection> getConnection(Long userId) {
        return stravaRepo.findByUserId(userId)
                .map(StravaConnection::from);
    }

    public List<StravaActivity> getRecentActivities(Long userId, int limit) {
        var integration = stravaRepo.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Strava not connected"));

        var token = tokenRefresher.getValidAccessToken(integration);

        return apiClient.getAthleteActivities(token, limit).stream()
                .map(StravaActivity::from)
                .toList();
    }
}
