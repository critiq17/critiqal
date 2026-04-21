package org.acme.infra.strava;

import org.acme.api.dtos.strava.StravaActivityDTO;

import java.time.Instant;

public record StravaActivity(
        Long id,
        String name,
        String type,
        double distanceMeters,
        int movingTimeSeconds,
        double elevationGain,
        Instant startDate,
        double avgHeartrate,
        double avgSpeedMs
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
                a.avgHeartrate(),
                Math.round(paceMinPerKm * 100.0) / 100.0
        );
    }
}