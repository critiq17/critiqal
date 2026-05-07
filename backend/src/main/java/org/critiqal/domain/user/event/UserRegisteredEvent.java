package org.critiqal.domain.user.event;

import java.util.UUID;

public record UserRegisteredEvent(UUID userId, String username) {}
