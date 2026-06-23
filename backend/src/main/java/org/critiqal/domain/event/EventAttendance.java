package org.critiqal.domain.event;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.critiqal.domain.shared.uuid.UuidGeneration;
import org.critiqal.domain.user.User;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_attendances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "user_id"}))
public class EventAttendance extends PanacheEntityBase {

    @Id
    @Column(columnDefinition = "uuid")
    public UUID id = UuidGeneration.generate();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    public Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public RsvpStatus status = RsvpStatus.GOING;

    @Column(name = "checked_in", nullable = false)
    public boolean checkedIn = false;

    @Column(name = "created_at", updatable = false)
    public Instant createdAt = Instant.now();
}
