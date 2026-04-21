package org.acme.infra.strava;

public record StravaTokenResponse(
        String accessToken,
        String refreshToken,
        Long expiresAt,
        StravaAthleteInfo athlete
) {}
