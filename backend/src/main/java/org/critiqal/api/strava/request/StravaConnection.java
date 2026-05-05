package org.critiqal.api.strava.request;

import org.critiqal.domain.strava.StravaIntegration;

import java.time.Instant;

public record StravaConnection(
        Long athleteId,
        String username,
        String firstname,
        String lastname,
        String city,
        String avatarUrl,
        Instant connectedAt
) {
    public static StravaConnection from(StravaIntegration i) {
        return new StravaConnection(
                i.athleteId,
                i.athleteUsername,
                i.athleteFirstname,
                i.athleteLastname,
                i.athleteCity,
                i.athleteAvatarUrl,
                i.connectedAt
        );
    }
}
