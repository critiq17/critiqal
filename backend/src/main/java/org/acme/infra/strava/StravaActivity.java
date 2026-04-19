package org.acme.infra.strava;

import java.time.Instant;

public record StravaActivity(
        Long id,
        String name,
        String type,
        double distanceMeters,
        int movingTimeSeconds,
        double elevationGain,
        Instant startDate,
        double angHeartrate,
        double angSpeedMs
) {}
