package org.critiqal.domain.badge;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.critiqal.domain.shared.uuid.UuidGeneration;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "badges")
public class Badge extends PanacheEntityBase {

    @Id
    @Column(columnDefinition = "uuid")
    public UUID id = UuidGeneration.generate();

    @Column(nullable = false, unique = true, length = 64)
    public String code;

    @Column(nullable = false, length = 128)
    public String name;

    @Column(columnDefinition = "TEXT")
    public String description;

    @Column(name = "icon_url", length = 512)
    public String iconUrl;

    @Column(name = "created_at", updatable = false)
    public Instant createdAt = Instant.now();
}
