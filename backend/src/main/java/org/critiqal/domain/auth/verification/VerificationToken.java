package org.critiqal.domain.auth.verification;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.critiqal.domain.shared.uuid.UuidGeneration;
import org.critiqal.domain.user.User;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verification_tokens")
public class VerificationToken extends PanacheEntityBase {

    @Id
    @Column(columnDefinition = "uuid")
    public UUID id = UuidGeneration.generate();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @Column(name = "token_hash", nullable = false)
    public String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public VerificationTokenType type;

    @Column
    public String email;

    @Column(name = "expires_at", nullable = false)
    public Instant expiresAt;

    @Column(name = "used_at")
    public Instant usedAt;

    @Column(name = "created_at", updatable = false)
    public Instant createdAt = Instant.now();

    public boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    public boolean isUsed() { return usedAt != null; }
    public boolean isValid() { return !isExpired() && !isUsed(); }
}
