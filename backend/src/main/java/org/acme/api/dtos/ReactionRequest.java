package org.acme.api.dtos;

import org.acme.domain.reaction.ReactionType;

public record ReactionRequest(ReactionType type) {}
