package org.critiqal.infra.auth.session;

import io.quarkus.test.junit.QuarkusTest;
import org.critiqal.domain.auth.session.SessionService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class RedisSessionServiceIT {

    private final SessionService sessions;

    public RedisSessionServiceIT(SessionService sessions) {
        this.sessions = sessions;
    }

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
