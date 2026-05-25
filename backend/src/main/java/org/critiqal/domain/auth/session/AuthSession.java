package org.critiqal.domain.auth.session;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.critiqal.domain.shared.uuid.UuidGeneration;
import org.critiqal.domain.user.User;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_sessions")
public class AuthSession extends PanacheEntityBase {

    @Id
    @Column(columnDefinition = "uuid")
    public UUID id = UuidGeneration.generate();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @Column(name = "session_id_hash", nullable = false, length = 64)
    public String sessionIdHash;

    @Column(name = "device_id_hash", length = 64)
    public String deviceIdHash;

    @Column(name = "ip_hash", length = 64)
    public String ipHash;

    @Column(name = "country_code", length = 2)
    public String countryCode;

    @Column(name = "city", length = 128)
    public String city;

    @Column(name = "user_agent")
    public String userAgent;

    @Column(name = "platform", length = 32)
    public String platform;

    @Column(name = "created_at", updatable = false)
    public Instant createdAt = Instant.now();

    @Column(name = "last_seen_at")
    public Instant lastSeenAt = Instant.now();

    @Column(name = "revoked_at")
    public Instant revokedAt;

    public boolean isActive() {
        return revokedAt == null;
    }
}
