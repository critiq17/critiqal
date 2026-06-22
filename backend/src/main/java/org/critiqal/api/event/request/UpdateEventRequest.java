package org.critiqal.api.event.request;

import org.critiqal.domain.event.EventLocationType;

import java.time.Instant;

public record UpdateEventRequest(
        String title,
        String description,
        String coverImageUrl,
        EventLocationType locationType,
        String locationValue,
        Instant startsAt,
        Instant endsAt,
        Integer capacity
) {}
