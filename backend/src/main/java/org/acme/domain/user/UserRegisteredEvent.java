package org.acme.domain.user;

public record UserRegisteredEvent(Long userId, String username) {}
