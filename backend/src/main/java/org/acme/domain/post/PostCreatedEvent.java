package org.acme.domain.post;

public record PostCreatedEvent(Long postId, Long authorId) {}
