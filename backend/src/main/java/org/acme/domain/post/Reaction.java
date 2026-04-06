package org.acme.domain.post;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;
import org.acme.domain.user.User;

@Entity
public class Reaction extends PanacheEntity {

    @ManyToOne public Post post;
    @ManyToOne public User user;

    @Enumerated(EnumType.STRING)
    public ReactionType type;

    @Table(uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"}))
}
