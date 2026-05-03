package org.critiqal.infra.strava;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.critiqal.domain.strava.StravaIntegration;

/*
    StravaTokenRefresher - refresher authorization token
 */

@ApplicationScoped
public class StravaTokenRefresher {

    private final StravaOAuthClient oAuthClient;

    public StravaTokenRefresher(StravaOAuthClient oAuthClient) {
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
