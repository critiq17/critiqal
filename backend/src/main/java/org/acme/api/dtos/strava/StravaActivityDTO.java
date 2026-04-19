package org.acme.api.dtos.strava;

import org.acme.infra.strava.StravaActivity;

import java.time.Instant;

public record StravaActivityDTO(
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
    public static StravaActivityDTO from(StravaActivity a) {
        double distanceKm = a.distanceMeters() / 1000.0;
        double paceMinPerKm = a.avgSpeedMs() > 0
                ? (1000.0 / a.avgSpeedMs()) / 60.0
                : 0;
        return new StravaActivityDTO(
                a.id(),
                a.name(),
                a.type(),
                Math.round(distanceKm * 100.0) / 100.0,
                a.movingTimeSeconds(),
                a.elevationGain(),
                a.startDate(),
                a.ab
        )
    }
}
