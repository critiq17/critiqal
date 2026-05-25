package org.critiqal.api.auth.response;

import org.critiqal.domain.auth.session.AuthSession;
import org.critiqal.infra.auth.metadata.CountryNames;
import org.critiqal.infra.auth.metadata.UserAgentParser;

import java.time.Instant;
import java.util.UUID;

public record SessionDto(
        UUID id,
        String platform,
        String browser,
        String countryCode,
        String countryName,
        String city,
        Instant createdAt,
        Instant lastSeenAt,
        boolean current
) {
    public static SessionDto from(AuthSession session, boolean isCurrent) {
        return new SessionDto(
                session.id,
                session.platform,
                UserAgentParser.browser(session.userAgent),
                session.countryCode,
                CountryNames.resolve(session.countryCode),
                session.city,
                session.createdAt,
                session.lastSeenAt,
                isCurrent
        );
    }
}
