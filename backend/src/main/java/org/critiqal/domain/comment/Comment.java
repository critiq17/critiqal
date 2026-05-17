package org.critiqal.domain.comment;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.critiqal.domain.post.Post;
import org.critiqal.domain.shared.uuid.UuidGeneration;
import org.critiqal.domain.user.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "comments")
public class Comment extends PanacheEntityBase {

    @Id
    @Column(columnDefinition = "uuid")
    public UUID id = UuidGeneration.generate();

    @ManyToOne public Post post;
    @ManyToOne public User author;
    public String content;
    public Instant createdAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    public Comment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    public List<Comment> replies;

}
