package org.acme.domain.reaction;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import org.acme.domain.post.Post;
import org.acme.domain.user.User;

@Entity
@Table(
        name = "reactions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"})
)
public class Reaction extends PanacheEntity {

    @ManyToOne public Post post;
    @ManyToOne public User user;

    @Enumerated(EnumType.STRING)
    public ReactionType type;
}
