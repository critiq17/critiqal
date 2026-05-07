package org.critiqal.domain.reaction.repository;

import org.critiqal.domain.reaction.Reaction;
import org.critiqal.domain.reaction.ReactionType;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Defines persistence operations for post reactions.
 * Supports per-user lookup, aggregation, and mutation.
 */
public interface ReactionRepository {

    Optional<Reaction> findByPostAndUser(UUID postId, UUID userId);

    Map<ReactionType, Long> countByPost(UUID postId);

    Reaction save(Reaction reaction);

    void deleteByPostAndUser(UUID postId, UUID userId);
}
