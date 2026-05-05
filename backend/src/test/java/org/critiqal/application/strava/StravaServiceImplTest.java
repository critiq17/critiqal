package org.critiqal.application.strava;

import org.critiqal.api.strava.request.StravaConnection;
import org.critiqal.domain.shared.exception.NotFoundException;
import org.critiqal.domain.strava.StravaIntegration;
import org.critiqal.domain.strava.gateway.Strava0AuthClient;
import org.critiqal.domain.strava.repository.StravaRepository;
import org.critiqal.domain.strava.service.StravaServiceImpl;
import org.critiqal.domain.user.User;
import org.critiqal.domain.user.service.UserService;
import org.critiqal.infra.strava.StravaActivity;
import org.critiqal.infra.strava.StravaApiClient;
import org.critiqal.infra.strava.StravaAthleteInfo;
import org.critiqal.infra.strava.StravaTokenRefresher;
import org.critiqal.infra.strava.StravaTokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StravaServiceImplTest {

    private final StravaRepository stravaRepo = mock(StravaRepository.class);
    private final Strava0AuthClient oAuthClient = mock(Strava0AuthClient.class);
    private final StravaApiClient apiClient = mock(StravaApiClient.class);
    private final StravaTokenRefresher tokenRefresher = mock(StravaTokenRefresher.class);
    private final UserService userService = mock(UserService.class);

    private StravaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StravaServiceImpl(stravaRepo, oAuthClient, apiClient, tokenRefresher, userService);
    }

    @Test
    void getAuthorizationUrlDelegatesToOAuthClient() {
        when(oAuthClient.buildAuthorizationUrl("15")).thenReturn("https://strava/auth");

        var url = service.getAuthorizationUrl(15L);

        assertEquals("https://strava/auth", url);
    }

    @Test
    void handleCallbackCreatesAndSavesNewIntegration() {
        var user = new User();
        user.id = 8L;
        var athlete = new StravaAthleteInfo(44L, "athlete", "Ada", "Lovelace", "London", "https://avatar");
        var tokenResponse = new StravaTokenResponse("access", "refresh", 12345L, athlete);

        when(oAuthClient.exchangeCode("code-1")).thenReturn(tokenResponse);
        when(userService.getById(8L)).thenReturn(user);
        when(stravaRepo.findByUserId(8L)).thenReturn(Optional.empty());

        service.handleCallback(8L, "code-1");

        var captor = ArgumentCaptor.forClass(StravaIntegration.class);
        verify(stravaRepo).save(captor.capture());
        var saved = captor.getValue();
        assertSame(user, saved.user);
        assertEquals("access", saved.accessToken);
        assertEquals("refresh", saved.refreshToken);
        assertEquals(12345L, saved.expiresAt);
        assertEquals(44L, saved.athleteId);
        assertEquals("athlete", saved.athleteUsername);
        assertEquals("Ada", saved.athleteFirstname);
        assertEquals("Lovelace", saved.athleteLastname);
        assertEquals("London", saved.athleteCity);
        assertEquals("https://avatar", saved.athleteAvatarUrl);
    }

    @Test
    void handleCallbackUpdatesExistingIntegrationInstance() {
        var existing = new StravaIntegration();
        existing.id = 91L;

        var user = new User();
        user.id = 8L;
        var athlete = new StravaAthleteInfo(44L, "athlete", "Ada", "Lovelace", "London", "https://avatar");
        var tokenResponse = new StravaTokenResponse("access", "refresh", 12345L, athlete);

        when(oAuthClient.exchangeCode("code-2")).thenReturn(tokenResponse);
        when(userService.getById(8L)).thenReturn(user);
        when(stravaRepo.findByUserId(8L)).thenReturn(Optional.of(existing));

        service.handleCallback(8L, "code-2");

        verify(stravaRepo).save(existing);
        assertSame(user, existing.user);
        assertEquals("access", existing.accessToken);
    }

    @Test
    void disconnectDeletesExistingIntegration() {
        var integration = new StravaIntegration();

        when(stravaRepo.findByUserId(5L)).thenReturn(Optional.of(integration));

        service.disconnect(5L);

        verify(stravaRepo).delete(integration);
    }

    @Test
    void disconnectSkipsDeleteWhenMissing() {
        when(stravaRepo.findByUserId(5L)).thenReturn(Optional.empty());

        service.disconnect(5L);

        verify(stravaRepo, never()).delete(any());
    }

    @Test
    void getConnectionMapsIntegrationToDto() {
        var integration = new StravaIntegration();
        integration.athleteId = 22L;
        integration.athleteUsername = "runner";
        integration.athleteFirstname = "Jane";
        integration.athleteLastname = "Doe";
        integration.athleteCity = "Prague";
        integration.athleteAvatarUrl = "https://avatar";
        integration.connectedAt = Instant.parse("2025-01-01T00:00:00Z");

        when(stravaRepo.findByUserId(8L)).thenReturn(Optional.of(integration));

        var connection = service.getConnection(8L).orElseThrow();

        assertEquals(new StravaConnection(22L, "runner", "Jane", "Doe", "Prague", "https://avatar", integration.connectedAt), connection);
    }

    @Test
    void getRecentActivitiesThrowsWhenNotConnected() {
        when(stravaRepo.findByUserId(8L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getRecentActivities(8L, 3));
    }

    @Test
    void getRecentActivitiesRefreshesTokenAndMapsApiActivities() {
        var integration = new StravaIntegration();
        var apiActivity = new StravaActivity(
                7L,
                "Morning Run",
                "Run",
                12_345.0,
                3600,
                123.4,
                Instant.parse("2025-02-01T08:00:00Z"),
                150.0,
                3.0
        );

        when(stravaRepo.findByUserId(8L)).thenReturn(Optional.of(integration));
        when(tokenRefresher.getValidAccessToken(integration)).thenReturn("token-1");
        when(apiClient.getAthleteActivities("token-1", 2)).thenReturn(List.of(apiActivity));

        var activities = service.getRecentActivities(8L, 2);

        assertEquals(1, activities.size());
        assertEquals(7L, activities.getFirst().id());
        assertEquals(12.35, activities.getFirst().distanceKm(), 0.001);
        assertEquals(5.56, activities.getFirst().avgPaceMinPerKm(), 0.001);
        verify(apiClient).getAthleteActivities("token-1", 2);
    }
}
