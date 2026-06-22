package org.critiqal.domain.event;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.critiqal.domain.shared.uuid.UuidGeneration;
import org.critiqal.domain.user.User;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
public class Event extends PanacheEntityBase {

    @Id
    @Column(columnDefinition = "uuid")
    public UUID id = UuidGeneration.generate();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    public User host;

    @Column(nullable = false, length = 140)
    public String title;

    @Column(columnDefinition = "TEXT")
    public String description;

    @Column(name = "cover_image_url", length = 512)
    public String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false, length = 24)
    public EventLocationType locationType = EventLocationType.DISCORD;

    @Column(name = "location_value", length = 512)
    public String locationValue;

    @Column(name = "starts_at", nullable = false)
    public Instant startsAt;

    @Column(name = "ends_at")
    public Instant endsAt;

    @Column
    public Integer capacity; // null = unlimited

    @Column(name = "attendee_count", nullable = false)
    public int attendeeCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public EventStatus status = EventStatus.DRAFT;

    @Column(name = "discord_guild_id", length = 32)
    public String discordGuildId;

    @Column(name = "discord_event_id", length = 32)
    public String discordEventId;

    @Column(name = "created_at", updatable = false)
    public Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    public Instant updatedAt = Instant.now();

    public UUID getId() {
        return id;
    }

    public boolean isManageableBy(UUID userId) {
        return host != null && userId != null && host.id.equals(userId);
    }

    public boolean isOpenForRsvp() {
        return status == EventStatus.PUBLISHED || status == EventStatus.LIVE;
    }
}
