package org.critiqal.infra.strava;

public record StravaAthleteInfo(
        Long id,
        String username,
        String firstName,
        String lastName,
        String city,
        String profileUrl
) {}
