package org.critiqal.domain.event.service;

import org.critiqal.domain.event.Event;
import org.critiqal.domain.event.EventAttendance;
import org.critiqal.domain.event.RsvpStatus;
import org.critiqal.domain.shared.pagination.Page;

import java.util.Optional;
import java.util.UUID;

public interface EventService {

    Event createEvent(UUID hostId, CreateEventCommand cmd);

    Event updateEvent(UUID eventId, UUID requesterId, UpdateEventCommand cmd);

    Event publishEvent(UUID eventId, UUID requesterId);

    Event cancelEvent(UUID eventId, UUID requesterId);

    void deleteEvent(UUID eventId, UUID requesterId);

    Event getById(UUID eventId);

    Page<Event> getUpcoming(int page, int size);

    Page<Event> getByHost(UUID hostId, int page, int size);

    Page<EventAttendance> getAttendees(UUID eventId, int page, int size);

    RsvpResult rsvp(UUID eventId, UUID userId, RsvpStatus status);

    void cancelRsvp(UUID eventId, UUID userId);

    Optional<RsvpStatus> viewerStatus(UUID eventId, UUID userId);

    // lifecycle (called by EventLifecycleJob)
    int startDueEvents();

    int endDueEvents();
}
