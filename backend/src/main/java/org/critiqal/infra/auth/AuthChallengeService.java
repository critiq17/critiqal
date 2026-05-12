package org.critiqal.infra.auth;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis-based one-time 2FA challenge.
 * create() -> client stores token -> verifyLogin/2fa sends if back -> consume() (GETDEL, atomic)
 */
@ApplicationScoped
public class AuthChallengeService {

    private static final String PREFIX = "auth:challenge:";

    private final ValueCommands<String, String> commands;
    private final long ttlSeconds;
    private final SecureRandom rng = new SecureRandom();

    public AuthChallengeService(RedisDataSource ds,
                                @ConfigProperty(name = "auth.2fa-challenge.ttl-seconds", defaultValue = "300") int ttl) {
        this.commands = ds.value(String.class);
        this.ttlSeconds = ttl;
    }

    public String create(UUID userId) {
        var token = token();
        commands.setex(PREFIX + token, ttlSeconds, userId.toString());
        return token;
    }

    public Optional<UUID> consume(String challengeToken) {
        if (challengeToken == null || challengeToken.isBlank()) return Optional.empty();
        var val = commands.getdel(PREFIX + challengeToken);
        if (val == null) return Optional.empty();
        try { return Optional.of(UUID.fromString(val)); }
        catch (IllegalArgumentException e) { return Optional.empty(); }
    }

    private String token() {
        var b = new byte[32]; rng.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
