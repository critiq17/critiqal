package org.critiqal.infra.auth.session;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.GetExArgs;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.inject.IllegalProductException;
import org.critiqal.domain.auth.session.AuthSession;
import org.critiqal.domain.auth.session.SessionService;
import org.critiqal.domain.auth.session.repository.AuthSessionRepository;
import org.critiqal.domain.user.repository.UserRepository;
import org.critiqal.infra.auth.metadata.RequestMetadata;
import org.critiqal.infra.auth.metadata.RequestMetadataResolver;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RedisSessionService implements SessionService {

    private static final Logger log = Logger.getLogger(RedisSessionService.class);

    private static final String SESSION_KEY_PREFIX = "session:";
    private static final String TOUCHED_KEY_PREFIX = "session:touched:";
    private static final long TOUCH_TTL_SECONDS = 15 * 60L;
    private static final int RANDOM_BYTES = 32;

    private final ValueCommands<String, String> commands;
    private final SecureRandom random = new SecureRandom();
    private final Duration ttl;
    private final AuthSessionRepository auditRepo;
    private final UserRepository userRepo;
    private final RequestMetadataResolver metadataResolver;

    public RedisSessionService(
            RedisDataSource ds,
            @ConfigProperty(name = "session.ttl-days") int ttlDays,
            AuthSessionRepository auditRepo,
            UserRepository userRepo,
            RequestMetadataResolver metadataResolver) {
        this.commands = ds.value(String.class);
        this.ttl = Duration.ofDays(ttlDays);
        this.auditRepo = auditRepo;
        this.userRepo = userRepo;
        this.metadataResolver = metadataResolver;
    }

    @Override
    public String create(UUID userId) {
        Objects.requireNonNull(userId, "userId");
        var sid = generateId();
        var sessionHash = RequestMetadataResolver.hash(sid);

        commands.setex(sessionKey(sessionHash), ttl.toSeconds(), userId.toString());

        saveAuditRecord(userId, sessionHash, resolveMetadataSafely());

        return sid;
    }

    @Override
    public Optional<UUID> resolve(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return Optional.empty();

        var sessionHash = RequestMetadataResolver.hash(sessionId);

        var value = commands.getex(
                sessionKey(sessionHash),
                new GetExArgs().ex(ttl.toSeconds())
        );
        if (value == null) return Optional.empty();

        try {
            var userId = UUID.fromString(value);
            touchLastSeen(sessionHash);
            return Optional.of(userId);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public void destroy(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return;
        destroyBySessionHash(RequestMetadataResolver.hash(sessionId));
    }

    @Override
    public boolean revoke(UUID userId, UUID authSessionId) {
        return auditRepo.findActiveByIdAndUserId(authSessionId, userId)
                .map(session -> {
                    destroyBySessionHash(session.sessionIdHash);
                    return true;
                })
                .orElse(false);
    }

    @Override
    public List<AuthSession> getSessions(UUID userId) {
        return auditRepo.findActiveByUserId(userId);
    }

    private void saveAuditRecord(UUID userId, String sessionHash, RequestMetadata metadata) {
        try {
            var user = userRepo.findByIdOptional(userId).orElse(null);
            if (user == null) return;

            var record = new AuthSession();
            record.user = user;
            record.sessionIdHash = sessionHash;
            record.deviceIdHash = metadata.deviceIdHash();
            record.ipHash = metadata.ipHash();
            record.countryCode = metadata.countryCode();
            record.city = metadata.city();
            record.userAgent = metadata.userAgent();
            record.platform = metadata.platform();
            auditRepo.save(record);
        } catch (Exception e) {
            log.warnf("Failed to write auth_sessions audit record for user %s: %s",
                    userId, e.getMessage());
        }
    }

    private void touchLastSeen(String sessionHash) {
        var touchKey = touchKey(sessionHash);
        if (commands.get(touchKey) != null) return;

        commands.setex(touchKey, TOUCH_TTL_SECONDS, "1");

        try {
            auditRepo.updateLastSeen(sessionHash, Instant.now());
        } catch (Exception e) {
            log.debugf("Failed to update last_seen for session: %s", e.getMessage());
        }
    }

    private void destroyBySessionHash(String sessionHash) {
        if (sessionHash == null || sessionHash.isBlank()) return;
        commands.getdel(sessionKey(sessionHash));
        commands.getdel(touchKey(sessionHash));
        revokeAuditRecord(sessionHash);
    }

    private void revokeAuditRecord(String sessionHash) {
        try {
            auditRepo.revoke(sessionHash, Instant.now());
        } catch (Exception e) {
            log.debugf("Failed to revoke auth_session audit record: %s", e.getMessage());
        }
    }

    private RequestMetadata resolveMetadataSafely() {
        try {
            return metadataResolver.resolve();
        } catch (ContextNotActiveException | IllegalProductException e) {
            return RequestMetadata.EMPTY;
        }
    }

    private String generateId() {
        var bytes = new byte[RANDOM_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sessionKey(String sessionHash) {
        return SESSION_KEY_PREFIX + sessionHash;
    }

    private String touchKey(String sessionHash) {
        return TOUCHED_KEY_PREFIX + sessionHash;
    }
}
