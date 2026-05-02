package org.critiqal.api.dtos.strava;

import org.critiqal.domain.strava.StravaIntegration;

import java.time.Instant;

public record StravaConnectionDTO(
        Long athleteId,
        String username,
        String firstname,
        String lastname,
        String city,
        String avatarUrl,
        Instant connectedAt
) {
    public static StravaConnectionDTO from(StravaIntegration i) {
        return new StravaConnectionDTO(
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
