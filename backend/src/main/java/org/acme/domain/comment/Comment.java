package org.acme.domain.comment;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import org.acme.domain.post.Post;
import org.acme.domain.user.User;

import java.time.Instant;

@Entity
public class Comment extends PanacheEntity {

    @ManyToOne public Post post;
    @ManyToOne public User author;
    public String content;
    public Instant createdAt = Instant.now();
}
