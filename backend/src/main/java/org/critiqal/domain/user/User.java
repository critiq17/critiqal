package org.critiqal.domain.user;


import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.critiqal.domain.follow.Follow;
import org.critiqal.domain.post.Post;
import org.critiqal.domain.shared.uuid.UuidGeneration;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User extends PanacheEntityBase {

    @Id
    @Column(columnDefinition = "uuid")
    public UUID id = UuidGeneration.generate();

    @Column(unique = true, nullable = false)
    public String username;

    @Column(nullable = false)
    public String passwordHash;

    public String name;
    public String bio;

    @Column(name = "avatar_url")
    public String avatarUrl;

    @Column(unique = true)
    public String email;

    @Column(name = "email_verified", nullable = false)
    public boolean emailVerified = false;

    @Column(name = "two_factor_enabled", nullable = false)
    public boolean twoFactorEnabled = false;

    @Column(updatable = false,
            name = "created_at"
    )
    public Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL)
    public List<Post> posts;

    @OneToMany(mappedBy = "follower")
    public List<Follow> following;
}
