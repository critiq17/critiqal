package org.critiqal.domain.post_photo;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.critiqal.domain.post.Post;
import org.critiqal.domain.shared.uuid.UuidGeneration;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "post_photos")
public class PostPhoto extends PanacheEntityBase {

    @Id
    @Column(columnDefinition = "uuid")
    public UUID id = UuidGeneration.generate();

    @ManyToOne(fetch = FetchType.LAZY)
    public Post post;

    public String url;
    public int position;
    public Instant createdAt = Instant.now();
}
