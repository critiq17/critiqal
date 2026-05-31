package org.critiqal.domain.ban;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.critiqal.domain.shared.uuid.UuidGeneration;
import org.critiqal.domain.user.User;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_bans")
public class UserBan extends PanacheEntityBase {

    @Id
    @Column(columnDefinition = "uuid")
    public UUID id = UuidGeneration.generate();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String reason;

    @Column(name = "banned_by", nullable = false, columnDefinition = "uuid")
    public UUID bannedBy;

    @Column(name = "created_at", updatable = false)
    public Instant createdAt = Instant.now();

    @Column(name = "expires_at")
    public Instant expiresAt;

    @Column(name = "lifted_at")
    public Instant liftedAt;

    @Column(name = "lifted_by", columnDefinition = "uuid")
    public UUID liftedBy;

    public boolean isActive() {
        if (liftedAt != null) return false;
        return expiresAt == null || expiresAt.isAfter(Instant.now());
    }

}
