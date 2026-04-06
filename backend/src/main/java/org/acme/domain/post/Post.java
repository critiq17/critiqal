package org.acme.domain.post;

import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import org.acme.domain.user.User;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "posts")
public class Post extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    public User author;

    @Column(columnDefinition = "TEXT")
    public String content;

    public String photoUrl;
    public long viewCount = 0;

    @Enumerated(EnumType.STRING)
    public PostStatus status = PostStatus.PUBLISHED;

    @Column(updatable = false)
    public Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<Comment> comments;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<Reaction> reactions;

    public static List<Post> findByAuthor(User author) {
        return list("author = ?1 ORDER BY createdAt DESC", author);
    }
}
