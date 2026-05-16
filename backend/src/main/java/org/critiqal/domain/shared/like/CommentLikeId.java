package org.critiqal.domain.shared.like;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class CommentLikeId implements Serializable {

    @Column(name = "comment_id")
    public UUID commentId;

    @Column(name = "user_id")
    public UUID userId;

    public CommentLikeId() {}

    public CommentLikeId(UUID commentId, UUID userID) {
        this.commentId = commentId;
        this.userId = userID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommentLikeId that)) return false;
        return Objects.equals(commentId, that.commentId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commentId, userId);
    }
}
