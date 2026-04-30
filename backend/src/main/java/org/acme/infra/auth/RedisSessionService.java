package org.acme.infra.auth;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.GetExArgs;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.application.auth.SessionService;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
public class RedisSessionService implements SessionService {

    private static final String KEY_PREFIX = "session";
    private static final int RANDOM_BYTES = 32;

    private final ValueCommands<String, String> commands;
    private final SecureRandom random = new SecureRandom();
    private final Duration ttl;

    public RedisSessionService(
            RedisDataSource ds,
            @ConfigProperty(name = "session.ttl-days") int ttlDays) {
        this.commands = ds.value(String.class);
        this.ttl = Duration.ofDays(ttlDays);
    }

    @Override
    public String create(Long userId) {
        Objects.requireNonNull(userId, "userId");
        var id = generateId();
        commands.setex(KEY_PREFIX + id, ttl.toSeconds(), userId.toString());
        return id;
    }

    @Override
    public Optional<Long> resolve(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return Optional.empty();
        var value = commands.getex(KEY_PREFIX + sessionId, new GetExArgs().ex(ttl.toSeconds()));
        if (value == null) return Optional.empty();
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    public void destroy(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return;
        commands.getdel(KEY_PREFIX + sessionId);
    }

    private String generateId() {
        var bytes = new byte[RANDOM_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
