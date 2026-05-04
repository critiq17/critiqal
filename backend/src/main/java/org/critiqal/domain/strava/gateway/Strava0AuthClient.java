package org.critiqal.domain.strava.gateway;

import org.critiqal.infra.strava.StravaTokenResponse;

public interface Strava0AuthClient {
    public String buildAuthorizationUrl(String state);
    public StravaTokenResponse exchangeCode(String code);
    public StravaTokenResponse refreshToken(String tokenRefresh);
}
