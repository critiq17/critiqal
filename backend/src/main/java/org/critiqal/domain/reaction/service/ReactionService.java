package org.critiqal.domain.reaction.service;

import org.critiqal.domain.reaction.Reaction;
import org.critiqal.domain.reaction.ReactionType;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Defines reaction management operations for posts.
 * Handles aggregation, current-user state, and reaction updates.
 */
public interface ReactionService {

    Map<ReactionType, Long> getReactions(UUID postId);

    Optional<ReactionType> getMyReaction(UUID postId, UUID userId);

    Reaction react(UUID userId, UUID postId, ReactionType type);

    void removeReaction(UUID userId, UUID postId);
}
