package org.critiqal.domain.reaction;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import org.critiqal.domain.post.Post;
import org.critiqal.domain.user.User;

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
