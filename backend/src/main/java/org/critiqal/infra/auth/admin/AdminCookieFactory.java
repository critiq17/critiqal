package org.critiqal.infra.auth.admin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.NewCookie;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AdminCookieFactory {

    private final String name;
    private final boolean secure;
    private final int maxAge;

    public AdminCookieFactory(
            @ConfigProperty(name = "admin.session.cookie.name") String name,
            @ConfigProperty(name = "admin.session.cookie.secure") boolean secure,
            @ConfigProperty(name = "admin.session.ttl-minutes") int ttlMinutes) {
        this.name = name;
        this.secure = secure;
        this.maxAge = ttlMinutes * 60;
    }

    public String name() { return name; }

    public NewCookie issue(String sid) { return base().value(sid).maxAge(maxAge).build(); }
    public NewCookie expire() { return base().value("").maxAge(0).build(); }

    private NewCookie.Builder base() {
        return new NewCookie.Builder(name)
                .path("/")
                .httpOnly(true)
                .secure(secure)
                .sameSite(NewCookie.SameSite.STRICT);
    }
}
