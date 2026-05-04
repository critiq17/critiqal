package org.critiqal.domain.reaction.repository;

import org.critiqal.domain.reaction.Reaction;
import org.critiqal.domain.reaction.ReactionType;

import java.util.Map;
import java.util.Optional;

public interface ReactionRepository {

    Optional<Reaction> findByPostAndUser(Long postId, Long userId);

    Map<ReactionType, Long> countByPost(Long postId);

    Reaction save(Reaction reaction);

    void deleteByPostAndUser(Long postId, Long userId);
}
