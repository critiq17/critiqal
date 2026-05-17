package org.critiqal.domain.post;

import java.util.UUID;

public record PostCreatedEvent(UUID postId, UUID authorId) {}
