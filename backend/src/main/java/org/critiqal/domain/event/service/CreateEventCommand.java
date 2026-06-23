package org.critiqal.domain.event.service;

import org.critiqal.domain.event.EventLocationType;

import java.time.Instant;

public record CreateEventCommand(
        String title,
        String description,
        String coverImageUrl,
        EventLocationType locationType,
        String locationValue,
        Instant startsAt,
        Instant endsAt,
        Integer capacity,
        boolean publishNow
) {}
