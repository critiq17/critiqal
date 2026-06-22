package org.critiqal.domain.event.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.event.Event;
import org.critiqal.domain.event.EventStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class EventRepositoryImpl implements EventRepository, PanacheRepository<Event> {

    @Override
    @Transactional
    public Event save(Event event) {
        persist(event);
        return event;
    }

    @Override
    public Optional<Event> findByIdOptional(UUID id) {
        return find("id", id).firstResultOptional();
    }

    @Override
    public List<Event> findUpcoming(int page, int size) {
        return find("status in (?1, ?2)",
                Sort.by("startsAt").ascending(),
                EventStatus.PUBLISHED, EventStatus.LIVE)
                .page(Page.of(page, size))
                .list();
    }

    @Override
    public long countUpcoming() {
        return count("status in (?1, ?2)", EventStatus.PUBLISHED, EventStatus.LIVE);
    }

    @Override
    public List<Event> findByHost(UUID hostId, int page, int size) {
        return find("host.id = ?1", Sort.by("createdAt").descending(), hostId)
                .page(Page.of(page, size))
                .list();
    }

    @Override
    public long countByHost(UUID hostId) {
        return count("host.id", hostId);
    }

    @Override
    public List<Event> findByStatusStartingBefore(EventStatus status, Instant now) {
        return list("status = ?1 and startsAt <= ?2", status, now);
    }

    @Override
    public List<Event> findByStatusEndingBefore(List<EventStatus> statuses, Instant now) {
        return list("status in ?1 and endsAt is not null and endsAt <= ?2", statuses, now);
    }

    @Override
    @Transactional
    public void incrementAttendees(UUID eventId, int delta) {
        getEntityManager().createNativeQuery("""
                UPDATE events
                SET attendee_count = GREATEST(attendee_count + ?1, 0),
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?2
                """)
                .setParameter(1, delta)
                .setParameter(2, eventId)
                .executeUpdate();
    }
}
