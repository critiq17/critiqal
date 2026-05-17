package org.critiqal.infra.auth.session;

import io.quarkus.test.junit.QuarkusTest;
import org.critiqal.domain.auth.session.SessionService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class RedisSessionServiceIT {

    private final SessionService sessions;

    public RedisSessionServiceIT(SessionService sessions) {
        this.sessions = sessions;
    }

    @Test
    void create_thenResolve_returnsUserId() {
        var userId = uuid(42);
        var sid = sessions.create(userId);
        assertThat(sessions.resolve(sid)).contains(userId);
    }

    @Test
    void resolve_afterDestroy_returnsEmpty() {
        var sid = sessions.create(uuid(7));
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

    private UUID uuid(int value) {
        return UUID.fromString("00000000-0000-0000-0000-%012d".formatted(value));
    }
}
