package org.critiqal.domain.shared.like;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
public class PostLikeId implements Serializable {
    public UUID postId;
    public UUID userId;
}
