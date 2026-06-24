package org.critiqal.infra.auth;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import org.critiqal.domain.shared.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimiterTest {

    @Mock RedisDataSource ds;
    @Mock ValueCommands<String, String> val;
    @Mock KeyCommands<String> keys;

    RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        when(ds.value(String.class)).thenReturn(val);
        when(ds.key(String.class)).thenReturn(keys);
        rateLimiter = new RateLimiter(ds);
    }

    @Test
    void check_underLimit_passes() {
        when(val.get("testkey")).thenReturn("3");
        assertDoesNotThrow(() -> rateLimiter.check("testkey", 5, Duration.ofMinutes(15)));
    }

    @Test
    void check_atLimit_throwsDomainException() {
        when(val.get("testkey")).thenReturn("5");
        assertThrows(DomainException.class, () -> rateLimiter.check("testkey", 5, Duration.ofMinutes(15)));
    }

    @Test
    void check_newKey_setsExpiryAndValue() {
        when(val.get("testkey")).thenReturn(null);
        rateLimiter.check("testkey", 5, Duration.ofMinutes(15));
        verify(val).setex("testkey", 900L, "1");
    }

    @Test
    void check_existingKey_increments() {
        when(val.get("testkey")).thenReturn("3");
        rateLimiter.check("testkey", 5, Duration.ofMinutes(15));
        verify(val).incr("testkey");
    }

    @Test
    void key_buildsExpectedString() {
        var k = RateLimiter.key("login-user", "alice");
        assert k.equals("ratelogin-user:alice");
    }
}
