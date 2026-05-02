package org.critiqal.application.auth;

import java.util.Optional;

/*
    SessionService - interface for auth sessions
*/

public interface SessionService {
    String create(Long userId);
    Optional<Long> resolve(String sessionId);
    void destroy(String sessionId);
}
