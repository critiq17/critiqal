package org.critiqal.domain.like.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface CommentLikeRepository extends LikeRepository {

    Map<UUID, Long> countByCommentIds(List<UUID> commentIds);

    Set<UUID> likedCommentIds(UUID userId, List<UUID> commentIds);
}
