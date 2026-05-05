package org.critiqal.domain.post_photo;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.critiqal.domain.post.Post;

import java.time.Instant;

@Entity
@Table(name = "post_photos")
public class PostPhoto extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    public Post post;

    public String url;
    public int position;
    public Instant createdAt = Instant.now();
}
