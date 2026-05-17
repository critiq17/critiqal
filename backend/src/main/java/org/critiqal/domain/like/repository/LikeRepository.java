package org.critiqal.domain.like.repository;

import java.util.UUID;

public interface LikeRepository {
    boolean exists(UUID targetId, UUID userId);
    void save(UUID targetId, UUID userId);
    void remove(UUID targetId, UUID userId);
    long count(UUID targetId);
}
