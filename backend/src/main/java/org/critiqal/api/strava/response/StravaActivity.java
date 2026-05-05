package org.critiqal.api.strava.response;

import java.time.Instant;

public record StravaActivity(
        Long id,
        String name,
        String type,
        double distanceKm,
        int movingTimeSeconds,
        double elevationGain,
        Instant startDate,
        double avgHeartRate,
        double avgPaceMinPerKm
) {
    public static StravaActivity from(org.critiqal.infra.strava.StravaActivity a) {
        double distanceKm = a.distanceMeters() / 1000.0;
        double paceMinPerKm = a.avgSpeedMs() > 0
                ? (1000.0 / a.avgSpeedMs()) / 60.0
                : 0;
        return new StravaActivity(
                a.id(),
                a.name(),
                a.type(),
                Math.round(distanceKm * 100.0) / 100.0,
                a.movingTimeSeconds(),
                a.elevationGain(),
                a.startDate(),
                a.avgHeartrate(),
                Math.round(paceMinPerKm * 100.0) / 100.0
        );
    }
}
