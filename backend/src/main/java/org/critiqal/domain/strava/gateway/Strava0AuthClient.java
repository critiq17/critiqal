package org.critiqal.domain.strava.gateway;

import org.critiqal.infra.strava.StravaTokenResponse;

/**
 * Defines OAuth interactions with Strava.
 * Builds authorization URLs and exchanges or refreshes tokens.
 */
public interface Strava0AuthClient {
    public String buildAuthorizationUrl(String state);
    public StravaTokenResponse exchangeCode(String code);
    public StravaTokenResponse refreshToken(String tokenRefresh);
}
