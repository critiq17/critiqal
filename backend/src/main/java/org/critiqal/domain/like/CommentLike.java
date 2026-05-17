package org.critiqal.domain.like;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.critiqal.domain.shared.like.CommentLikeId;

import java.time.Instant;

@Entity @Table(name = "comment_likes")
public class CommentLike extends PanacheEntityBase {
        @EmbeddedId public CommentLikeId id;
        public Instant createdAt = Instant.now();
}
