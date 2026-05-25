package org.critiqal.domain.auth.session.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.auth.session.AuthSession;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AuthSessionRepositoryImpl
        implements AuthSessionRepository, PanacheRepository<AuthSession> {

    @Override
    @Transactional
    public AuthSession save(AuthSession session) {
        persist(session);
        return session;
    }

    @Override
    public Optional<AuthSession> findBySessionIdHash(String hash) {
        return find("sessionIdHash = ?1", hash).firstResultOptional();
    }

    @Override
    public List<AuthSession> findActiveByUserId(UUID userId) {
        return find(
                "user.id = ?1 AND revokedAt IS NULL ORDER BY lastSeenAt DESC",
                userId
        ).list();
    }

    @Override
    public Optional<AuthSession> findActiveByIdAndUserId(UUID sessionId, UUID userId) {
        return find(
                "id = ?1 AND user.id = ?2 AND revokedAt IS NULL",
                sessionId,
                userId
        ).firstResultOptional();
    }

    @Override
    public boolean existsByDeviceIdHash(String deviceIdHash) {
        if (deviceIdHash == null) return false;
        return count("deviceIdHash = ?1", deviceIdHash) > 0;
    }

    @Override
    public boolean existsByDeviceIdHashAndUserId(String deviceIdHash, UUID userId) {
        if (deviceIdHash == null) return false;
        return count("deviceIdHash = ?1 AND user.id = ?2", deviceIdHash, userId) > 0;
    }

    @Override
    @Transactional
    public void updateLastSeen(String sessionIdHash, Instant at) {
        update("lastSeenAt = ?1 WHERE sessionIdHash = ?2", at, sessionIdHash);
    }

    @Override
    @Transactional
    public void revoke(String sessionIdHash, Instant at) {
        update("revokedAt = ?1 WHERE sessionIdHash = ?2 AND revokedAt IS NULL",
                at, sessionIdHash);
    }
}
