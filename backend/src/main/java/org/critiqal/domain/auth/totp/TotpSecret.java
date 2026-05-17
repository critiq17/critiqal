package org.critiqal.domain.auth.totp;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.critiqal.domain.shared.uuid.UuidGeneration;
import org.critiqal.domain.user.User;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "totp_secrets")
public class TotpSecret extends PanacheEntityBase {

    @Id
    @Column(columnDefinition = "uuid")
    public UUID id = UuidGeneration.generate();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = true)
    public User user;

    @Column(name = "secret_encrypted", nullable = false, length = 512)
    public String secretEncrypted;

    @Column(nullable = false)
    public boolean confirmed = false;

    @Column(name = "created_at", updatable = false)
    public Instant createdAt = Instant.now();
}
