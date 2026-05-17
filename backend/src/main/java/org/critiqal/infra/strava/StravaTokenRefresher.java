package org.critiqal.infra.strava;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.strava.StravaIntegration;
import org.critiqal.domain.strava.gateway.Strava0AuthClient;

/**
 * Refreshes expired Strava access tokens and persists updated credentials.
 */
@ApplicationScoped
public class StravaTokenRefresher {

    private final Strava0AuthClient oAuthClient;

    public StravaTokenRefresher(Strava0AuthClient oAuthClient) {
        this.oAuthClient = oAuthClient;
    }

    // If token is expired - update in DB and return new
    @Transactional
    public String getValidAccessToken(StravaIntegration integration) {
        if (!integration.isTokenExpired()) {
            return integration.accessToken;
        }

        var refreshed = oAuthClient.refreshToken(integration.refreshToken);
        integration.accessToken = refreshed.accessToken();
        integration.refreshToken = refreshed.refreshToken();
        integration.expiresAt = refreshed.expiresAt();

        return refreshed.accessToken();
    }
}
