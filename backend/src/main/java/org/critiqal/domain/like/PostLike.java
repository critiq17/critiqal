package org.critiqal.domain.like;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.critiqal.domain.shared.like.PostLikeId;

import java.time.Instant;

@Entity @Table(name = "post_likes")
public class PostLike extends PanacheEntityBase {
    @EmbeddedId public PostLikeId id;
    public Instant createdAt = Instant.now();
}
