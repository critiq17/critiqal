package org.critiqal.domain.shared.like;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
public class CommentLikeId implements Serializable {
    public UUID commentId;
    public UUID userId;
}
