package org.critiqal.support;

import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;
import org.critiqal.infra.auth.RateLimiter;

import java.time.Duration;

// Disable rate limiting under @QuarkusTest. Integration tests deliberately
// hammer endpoints (multiple registers per test, multiple tests per class) —
// the production 5/hour cap is correct but turns the suite red. Real
// rate-limit behaviour is covered by dedicated unit tests, not this seam.
@Mock
@ApplicationScoped
public class TestRateLimiter extends RateLimiter {

    public TestRateLimiter() {
        super();
    }

    @Override
    public void check(String key, int max, Duration window) {
        // no-op
    }
}
