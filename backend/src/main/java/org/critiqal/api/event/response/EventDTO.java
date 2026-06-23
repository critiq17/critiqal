package org.critiqal.api.event.response;

import org.critiqal.domain.event.Event;
import org.critiqal.domain.event.RsvpStatus;

import java.time.Instant;

public record EventDTO(
        String id,
        EventHostDTO host,
        String title,
        String description,
        String coverImageUrl,
        String locationType,
        String locationValue,
        Instant startsAt,
        Instant endsAt,
        Integer capacity,
        int attendeeCount,
        String status,
        String discordEventId,
        Instant createdAt,
        String discordEventUrl,
        String viewerRsvp,   // GOING | INTERESTED | null
        boolean canManage
) {
    public static EventDTO from(Event e, RsvpStatus viewerRsvp, boolean canManage) {
        return new EventDTO(
                e.id.toString(),
                EventHostDTO.from(e.host),
                e.title,
                e.description,
                e.coverImageUrl,
                e.locationType.name(),
                e.locationValue,
                e.startsAt,
                e.endsAt,
                e.capacity,
                e.attendeeCount,
                e.status.name(),
                e.discordEventId,
                e.createdAt,
                (e.discordGuildId != null && e.discordEventId != null)
                        ? "https://discord.com/events/" + e.discordGuildId + "/" + e.discordEventId
                        : null,
                viewerRsvp != null ? viewerRsvp.name() : null,
                canManage
        );
    }
}
