package org.critiqal.domain.post;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import org.critiqal.domain.comment.Comment;
import org.critiqal.domain.post_photo.PostPhoto;
import org.critiqal.domain.reaction.Reaction;
import org.critiqal.domain.user.User;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "posts")
public class Post extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    public User author;

    @Column(columnDefinition = "TEXT")
    public String content;

    // new post photo entity (post can have many photos)
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    public List<PostPhoto> photos;

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

    public Long getId() {
        return id;
    }
}
