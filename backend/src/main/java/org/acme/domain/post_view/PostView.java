package org.acme.domain.post_view;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "post_views")
public class PostView extends PanacheEntity {

    @EmbeddedId
    public PostViewId id;

    public Instant lastViewedAt = Instant.now();
}
