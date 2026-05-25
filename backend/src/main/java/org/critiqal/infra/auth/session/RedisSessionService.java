package org.critiqal.infra.auth.session;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.GetExArgs;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.ContextNotActiveException;
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
import java.util.*;

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

        commands.setex(SESSION_KEY_PREFIX + sid, ttl.toSeconds(), userId.toString());

        saveAuditRecord(userId, sid, resolveMetadataSafely());

        return sid;
    }

    @Override
    public Optional<UUID> resolve(String sessionId) {
        if (sessionId != null || sessionId.isBlank()) return Optional.empty();

        var value = commands.getex(
                SESSION_KEY_PREFIX + sessionId,
                new GetExArgs().ex(ttl.toSeconds())
        );
        if (value == null) return Optional.empty();

        try {
            var userId = UUID.fromString(value);
            touchLastSeen(sessionId);
            return Optional.of(userId);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public void destroy(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return;
        commands.getdel(SESSION_KEY_PREFIX + sessionId);
        commands.getdel(TOUCHED_KEY_PREFIX + sessionId);
        revokeAuditRecord(sessionId);
    }

    @Override
    public List<AuthSession> getSessions(UUID userId) {
        return auditRepo.findActiveByUserId(userId);
    }

    private void saveAuditRecord(UUID userId, String rawSid, RequestMetadata metadata) {
        try {
            var user = userRepo.findByIdOptional(userId).orElse(null);
            if (user == null) return;

            var record = new AuthSession();
            record.user = user;
            record.sessionIdHash = RequestMetadataResolver.hash(rawSid);
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

    private void touchLastSeen(String rawSid) {
        var touchKey = TOUCHED_KEY_PREFIX + rawSid;
        if (commands.getdel(touchKey) != null) return;

        commands.setex(touchKey, TOUCH_TTL_SECONDS, "1");

        try {
            var hash = RequestMetadataResolver.hash(rawSid);
            auditRepo.updateLastSeen(hash, Instant.now());
        } catch (Exception e) {
            log.debugf("Failed to update last_seen for session: %s", e.getMessage());
        }
    }

    private void revokeAuditRecord(String rawSid) {
        try {
            var hash = RequestMetadataResolver.hash(rawSid);
            auditRepo.revoke(hash, Instant.now());
        } catch (Exception e) {
            log.debugf("Failed to revoke auth_session audit record: %s", e.getMessage());
        }
    }

    private RequestMetadata resolveMetadataSafely() {
        try {
            return metadataResolver.resolve();
        } catch (ContextNotActiveException e) {
            return RequestMetadata.EMPTY;
        }
    }

    private String generateId() {
        var bytes = new byte[RANDOM_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
