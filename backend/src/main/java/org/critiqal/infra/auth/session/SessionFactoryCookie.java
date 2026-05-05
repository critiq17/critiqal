package org.critiqal.infra.auth.session;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.NewCookie;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Locale;

@ApplicationScoped
public class SessionFactoryCookie {

    private final String name;
    private final boolean secure;
    private final NewCookie.SameSite sameSite;
    private final int maxAgeSeconds;

    public SessionFactoryCookie(
            @ConfigProperty(name = "session.cookie.name") String name,
            @ConfigProperty(name = "session.cookie.secure") boolean secure,
            @ConfigProperty(name = "session.cookie.same-site") String sameSite,
            @ConfigProperty(name = "session.ttl-days") int ttlDays) {
        this.name = name;
        this.secure = secure;
        this.sameSite = parseSameSite(sameSite);
        this.maxAgeSeconds = ttlDays * 24 * 60 * 60;

        if (this.sameSite == NewCookie.SameSite.NONE && !secure) {
            throw new IllegalArgumentException("session.cookie.same-site=None requires session.cookie.secure=true");
        }
    }

    public NewCookie issue(String sessionId) {
        return baseCookie()
                .value(sessionId)
                .maxAge(maxAgeSeconds)
                .build();
    }

    public NewCookie expire() {
        return baseCookie()
                .value("")
                .maxAge(0)
                .build();
    }

    private NewCookie.Builder baseCookie() {
        return new NewCookie.Builder(name)
                .path("/")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite);
    }

    private static NewCookie.SameSite parseSameSite(String sameSite) {
        return switch (sameSite.trim().toUpperCase(Locale.ROOT)) {
            case "LAX" -> NewCookie.SameSite.LAX;
            case "STRICT" -> NewCookie.SameSite.STRICT;
            case "NONE" -> NewCookie.SameSite.NONE;
            default -> throw new IllegalArgumentException(
                    "Unsupported session.cookie.same-site value: " + sameSite);
        };
    }
}
