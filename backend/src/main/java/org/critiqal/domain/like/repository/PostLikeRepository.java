package org.critiqal.domain.like.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface PostLikeRepository extends LikeRepository {

    Map<UUID, Long> countByPostIds(List<UUID> postIds);

    Set<UUID> likedPostIds(UUID userId, List<UUID> postIds);
}
