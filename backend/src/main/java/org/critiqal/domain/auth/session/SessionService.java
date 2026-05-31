package org.critiqal.domain.auth.session;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Defines lifecycle operations for authentication sessions.
 * Handles creation, resolution, and invalidation of session identifiers.
 */
public interface SessionService {
    String create(UUID userId);
    Optional<UUID> resolve(String sessionId);
    void destroy(String sessionId);

    boolean revoke(UUID userId, UUID authSessionId);
    void revokeAll(UUID userId);
    List<AuthSession> getSessions(UUID userId);
}
