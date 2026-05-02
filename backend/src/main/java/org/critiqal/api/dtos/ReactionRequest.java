package org.critiqal.api.dtos;

import org.critiqal.domain.reaction.ReactionType;

public record ReactionRequest(ReactionType type) {}
