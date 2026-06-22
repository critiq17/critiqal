package org.critiqal.domain.event.repository;

import org.critiqal.domain.event.Event;
import org.critiqal.domain.event.EventStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository {

    Event save(Event event);

    Optional<Event> findByIdOptional(UUID id);

    List<Event> findUpcoming(int page, int size);

    long countUpcoming();

    List<Event> findByHost(UUID hostId, int page, int size);

    long countByHost(UUID hostId);

    List<Event> findByStatusStartingBefore(EventStatus status, Instant now);

    List<Event> findByStatusEndingBefore(List<EventStatus> statuses, Instant now);

    void incrementAttendees(UUID eventId, int delta);
}
