package org.acme.infra.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.NewCookie;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SessionFactoryCookie {

    private final String name;
    private final boolean secure;
    private final int maxAgeSeconds;

    public SessionFactoryCookie(
            @ConfigProperty(name = "sessions.cookie.name") String name,
            @ConfigProperty(name = "sessions.cookie.secure") boolean secure,
            @ConfigProperty(name = "sessions.ttl-days") int ttlDays) {
        this.name = name;
        this.secure = secure;
        this.maxAgeSeconds = ttlDays * 24 * 60 * 60;
    }

    public NewCookie issue(String sessionId) {
        return new NewCookie.Builder(name)
                .value(sessionId)
                .path("/")
                .httpOnly(true)
                .secure(secure)
                .sameSite(NewCookie.SameSite.LAX)
                .maxAge(maxAgeSeconds)
                .build();
    }

    public NewCookie expire() {
        return new NewCookie.Builder(name)
                .value("")
                .path("/")
                .httpOnly(true)
                .secure(secure)
                .sameSite(NewCookie.SameSite.LAX)
                .maxAge(0)
                .build();
    }
}
