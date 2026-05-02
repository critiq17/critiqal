package org.critiqal.api;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.critiqal.application.auth.SessionService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class RedisSessionServiceIT {

    @Inject
    SessionService sessions;

    @Test
    void create_thenResolve_returnsUserId() {
        var sid = sessions.create(42L);
        assertThat(sessions.resolve(sid)).contains(42L);
    }

    @Test
    void resolve_afterDestroy_returnsEmpty() {
        var sid = sessions.create(7L);
        sessions.destroy(sid);
        assertThat(sessions.resolve(sid)).isEmpty();
    }

    @Test
    void resolve_unknownId_returnsEmpty() {
        assertThat(sessions.resolve("nonexistent")).isEmpty();
    }

    @Test
    void resolve_blankId_returnsEmpty() {
        assertThat(sessions.resolve("")).isEmpty();
        assertThat(sessions.resolve(null)).isEmpty();
    }
}
