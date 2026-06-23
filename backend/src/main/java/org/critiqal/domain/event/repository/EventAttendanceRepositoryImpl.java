package org.critiqal.domain.event.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.event.EventAttendance;
import org.critiqal.domain.event.RsvpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class EventAttendanceRepositoryImpl
        implements EventAttendanceRepository, PanacheRepository<EventAttendance> {

    @Override
    @Transactional
    public EventAttendance save(EventAttendance attendance) {
        persist(attendance);
        return attendance;
    }

    @Override
    public Optional<EventAttendance> findByEventAndUser(UUID eventId, UUID userId) {
        return find("event.id = ?1 and user.id = ?2", eventId, userId).firstResultOptional();
    }

    @Override
    public List<EventAttendance> findGoingByEvent(UUID eventId) {
        return find("event.id = ?1 and status = ?2", eventId, RsvpStatus.GOING)
                .list();
    }

    @Override
    public List<EventAttendance> findByEvent(UUID eventId, int page, int size) {
        return find("event.id = ?1", Sort.by("createdAt").ascending(), eventId)
                .page(Page.of(page, size))
                .list();
    }

    @Override
    public long countByEvent(UUID eventId) {
        return count("event.id", eventId);
    }

    @Override
    @Transactional
    public boolean delete(UUID eventId, UUID userId) {
        return delete("event.id = ?1 and user.id = ?2", eventId, userId) > 0;
    }
}
