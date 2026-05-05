package org.critiqal.domain.auth.session;

import java.util.Optional;

/**
 * Defines lifecycle operations for authentication sessions.
 * Handles creation, resolution, and invalidation of session identifiers.
 */
public interface SessionService {
    String create(Long userId);
    Optional<Long> resolve(String sessionId);
    void destroy(String sessionId);
}
