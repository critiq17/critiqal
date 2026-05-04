package org.critiqal.domain.user.event;

public record UserRegisteredEvent(Long userId, String username) {}
