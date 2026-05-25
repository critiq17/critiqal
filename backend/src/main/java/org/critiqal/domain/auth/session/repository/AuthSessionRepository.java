package org.critiqal.domain.auth.session.repository;

import org.critiqal.domain.auth.session.AuthSession;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthSessionRepository {

    AuthSession save(AuthSession session);

    Optional<AuthSession> findBySessionIdHash(String sessionIdHash);

    List<AuthSession> findActiveByUserId(UUID userId);

    Optional<AuthSession> findActiveByIdAndUserId(UUID sessionId, UUID userId);

    /**
     * Multi-account guard: returns true if the device has been used to create
     * ANY account. Device ID must be pre-hashed.
     */
    boolean existsByDeviceIdHash(String deviceIdHash);

    /**
     * New-device login alert: returns true if the device has been seen for
     * THIS specific user before.
     */
    boolean existsByDeviceIdHashAndUserId(String deviceIdHash, UUID userId);

    /** Throttled heartbeat - called at most once per 15 minutes per session. */
    void updateLastSeen(String sessionIdHash, Instant at);

    /** Marks the session as revoked without deleting the audit row. */
    void revoke(String sessionIdHash, Instant at);
}
