package org.critiqal.api.auth.response;

import org.critiqal.domain.auth.session.AuthSession;

import java.time.Instant;
import java.util.UUID;

public record SessionDto(
        UUID id,
        String platform,
        String countryCode,
        Instant createdAt,
        Instant lastSeenAt,
        boolean current
) {
    public static SessionDto from(AuthSession session, boolean isCurrent) {
        return new SessionDto(
                session.id,
                session.platform,
                session.countryCode,
                session.createdAt,
                session.lastSeenAt,
                isCurrent
        );
    }
}
