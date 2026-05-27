package org.critiqal.infra.auth;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.critiqal.domain.shared.exception.DomainException;

import java.time.Duration;

@ApplicationScoped
public class RateLimiter {

    private final ValueCommands<String, String> val;
    private final KeyCommands<String> keys;

    @Inject
    public RateLimiter(RedisDataSource ds) {
        this.val = ds.value(String.class);
        this.keys = ds.key(String.class);
    }

    // Required so the normal-scoped CDI proxy (and test subclasses) can be generated.
    // @Inject above tells ArC to use the RedisDataSource ctor for the real bean instance;
    // without it ArC silently picks this no-arg ctor and `val` stays null.
    protected RateLimiter() {
        this.val = null;
        this.keys = null;
    }

    public void check(String key, int max, Duration window) {
        var raw = val.get(key);
        var current = raw == null ? 0 : Integer.parseInt(raw);
        if (current >= max) throw new DomainException("Too many attempts. Please try again later.");
        if (raw == null) { val.setex(key, window.toSeconds(), "1"); }
        else { val.incr(key); }
    }
    public static String key(String action, String id) { return "rate" + action + ":" + id; }
}
