package org.critiqal.domain.reaction.service;

import org.critiqal.domain.reaction.Reaction;
import org.critiqal.domain.reaction.ReactionType;

import java.util.Map;
import java.util.Optional;

/**
 * Defines reaction management operations for posts.
 * Handles aggregation, current-user state, and reaction updates.
 */
public interface ReactionService {

    Map<ReactionType, Long> getReactions(Long postId);

    Optional<ReactionType> getMyReaction(Long postId, Long userId);

    Reaction react(Long userId, Long postId, ReactionType type);

    void removeReaction(Long userId, Long postId);
}
