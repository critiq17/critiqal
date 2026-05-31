package org.critiqal.infra.auth.admin;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.GetExArgs;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import org.critiqal.infra.auth.metadata.RequestMetadataResolver;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;


@ApplicationScoped
public class AdminSessionService {

    private static final String PREFIX = "admin:session";
    private final ValueCommands<String, String> redis;
    private final SecureRandom random = new SecureRandom();
    private final long ttlSeconds;

    public AdminSessionService(RedisDataSource ds,
                               @ConfigProperty(name = "admin.session.ttl-minutes") int ttlMinutes) {
        this.redis = ds.value(String.class);
        this.ttlSeconds = ttlMinutes * 60L;
    }

    public String create(UUID userId) {
        var sid = generateId();
        redis.setex(key(sid), ttlSeconds, userId.toString());
        return sid;
    }

    public Optional<UUID> resolve(String sid) {
        if (sid == null || sid.isBlank()) return Optional.empty();
        var val = redis.getex(key(sid), new GetExArgs().ex(ttlSeconds));
        if (val == null) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(val));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public void destroy(String sid) {
        if (sid != null && !sid.isBlank()) redis.getdel(key(sid));
    }

    private String key(String sid) {
        return PREFIX + RequestMetadataResolver.hash(sid);
    }

    private String generateId() {
        var b = new byte[32];
        random.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
