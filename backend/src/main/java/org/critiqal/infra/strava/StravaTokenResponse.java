package org.critiqal.infra.strava;

public record StravaTokenResponse(
        String accessToken,
        String refreshToken,
        Long expiresAt,
        StravaAthleteInfo athlete
) {}
