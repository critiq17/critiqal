package org.critiqal.domain.like.service;

import org.critiqal.domain.like.LikeResult;

import java.util.UUID;

public interface LikeService {

    LikeResult toggle(UUID targetId, UUID userId);

    long count(UUID targetId);

    boolean isLiked(UUID targetId, UUID userId);
}
