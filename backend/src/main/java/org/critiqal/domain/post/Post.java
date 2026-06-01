package org.critiqal.domain.post;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.critiqal.domain.comment.Comment;
import org.critiqal.domain.post_photo.PostPhoto;
import org.critiqal.domain.shared.uuid.UuidGeneration;
import org.critiqal.domain.user.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "posts")
public class Post extends PanacheEntityBase {

    @Id
    @Column(columnDefinition = "uuid")
    public UUID id = UuidGeneration.generate();

    @ManyToOne(fetch = FetchType.LAZY)
    public User author;

    @Column(columnDefinition = "TEXT")
    public String content;

    // new post photo entity (post can have many photos)
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    public List<PostPhoto> photos;

    public long viewCount = 0;

    @Column(name = "like_count", nullable = false)
    public int likeCount = 0;

    @Column(name = "comment_count", nullable = false)
    public int commentCount = 0;

    @Enumerated(EnumType.STRING)
    public PostStatus status = PostStatus.PUBLISHED;

    @Column(updatable = false)
    public Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<Comment> comments;

    public static List<Post> findByAuthor(User author) {
        return list("author = ?1 ORDER BY createdAt DESC", author);
    }

    public UUID getId() {
        return id;
    }
}
