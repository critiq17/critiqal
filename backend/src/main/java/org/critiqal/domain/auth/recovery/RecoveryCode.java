package org.critiqal.domain.auth.recovery;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.critiqal.domain.shared.uuid.UuidGeneration;
import org.critiqal.domain.user.User;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recovery_codes")
public class RecoveryCode extends PanacheEntityBase {

    @Id
    @Column(columnDefinition = "uuid")
    public UUID id = UuidGeneration.generate();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @Column(name = "code_hash", nullable = false)
    public String codeHash;

    @Column(name = "used_at")
    public Instant usedAt;

    @Column(name = "created_at", updatable = false)
    public Instant createdAt = Instant.now();

    public boolean isUsed() { return usedAt != null; }
}
