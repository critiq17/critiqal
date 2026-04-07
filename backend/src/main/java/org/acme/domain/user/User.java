package org.acme.domain.user;


import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import org.acme.domain.post.Post;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "users")
public class User extends PanacheEntity {

    @Column(unique = true, nullable = false)
    public String username;

    @Column(nullable = false)
    public String passwordHash;

    public String name;
    public String bio;
    public String avatarUrl;

    @Column(updatable = false)
    public Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL)
    public List<Post> posts;

    @OneToMany(mappedBy = "follower")
    public List<Follow> following;

    public static Optional<User> findByUsername(String username) {
        return find("username", username).firstResultOptional();
    }
}

