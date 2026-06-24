package org.critiqal.infra.auth;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.critiqal.domain.shared.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
class RateLimiterTest {

    @Inject
    RedisDataSource ds;

    // Construct directly to bypass the TestRateLimiter CDI mock.
    RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter(ds);
    }

    @Test
    void check_fiveCallsPass_sixthThrows() {
        String key = "test:rl:" + UUID.randomUUID();
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> rateLimiter.check(key, 5, Duration.ofMinutes(1)));
        }
        assertThrows(DomainException.class, () -> rateLimiter.check(key, 5, Duration.ofMinutes(1)));
    }

    @Test
    void check_afterWindowExpires_counterResets() throws InterruptedException {
        String key = "test:rl:" + UUID.randomUUID();
        assertDoesNotThrow(() -> rateLimiter.check(key, 1, Duration.ofSeconds(1)));
        assertThrows(DomainException.class, () -> rateLimiter.check(key, 1, Duration.ofSeconds(1)));

        Thread.sleep(1100);

        assertDoesNotThrow(() -> rateLimiter.check(key, 1, Duration.ofSeconds(1)));
    }
}
