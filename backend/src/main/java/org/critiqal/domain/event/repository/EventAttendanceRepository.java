package org.critiqal.domain.event.repository;

import org.critiqal.domain.event.EventAttendance;
import org.critiqal.domain.event.RsvpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventAttendanceRepository {

    EventAttendance save(EventAttendance attendance);

    Optional<EventAttendance> findByEventAndUser(UUID eventId, UUID userId);

    List<EventAttendance> findGoingByEvent(UUID eventId);

    List<EventAttendance> findByEvent(UUID eventId, int page, int size);

    long countByEvent(UUID eventId);

    boolean delete(UUID eventId, UUID userId);
}
