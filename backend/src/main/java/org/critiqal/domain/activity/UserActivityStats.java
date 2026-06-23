package org.critiqal.domain.activity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_activity_stats")
public class UserActivityStats extends PanacheEntityBase {

    @Id
    @Column(name = "user_id", columnDefinition = "uuid")
    public UUID userId;

    @Column(name = "posts_count", nullable = false)
    public long postsCount = 0;

    @Column(name = "comments_count", nullable = false)
    public long commentsCount = 0;

    @Column(name = "likes_count", nullable = false)
    public long likesCount = 0;

    @Column(name = "member_days", nullable = false)
    public int memberDays = 0;

    @Column(name = "events_hosted", nullable = false)
    public long eventsHosted = 0;

    @Column(name = "events_attended", nullable = false)
    public long eventsAttended = 0;

    @Column(name = "last_updated", nullable = false)
    public Instant lastUpdated = Instant.now();
}
