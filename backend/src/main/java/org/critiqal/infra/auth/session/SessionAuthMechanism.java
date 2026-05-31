package org.critiqal.infra.auth.session;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import org.critiqal.domain.auth.session.SessionService;
import org.critiqal.infra.auth.ban.BanCache;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Set;

@ApplicationScoped
public class SessionAuthMechanism implements HttpAuthenticationMechanism {

    private final SessionService sessions;
    private final BanCache banCache;
    private final String cookieName;

    public SessionAuthMechanism(
            SessionService sessions,
            BanCache banCache,
            @ConfigProperty(name = "session.cookie.name") String cookieName) {
        this.sessions = sessions;
        this.banCache = banCache;
        this.cookieName = SessionFactoryCookie.normalizeName(cookieName);
    }

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context,
            IdentityProviderManager idm) {
        if (shouldSkipAuthentication(context)) {
            return Uni.createFrom().nullItem();
        }

        var cookie = context.request().getCookie(cookieName);
        if (cookie == null)
            return Uni.createFrom().nullItem();
        var sid = cookie.getValue();

        return Uni.createFrom().item(() -> sessions.resolve(sid))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .map(opt -> opt
                        .filter(userId -> isBanExempt(context) || !banCache.isBanned(userId))
                        .map(userId -> (SecurityIdentity) QuarkusSecurityIdentity.builder()
                                .setPrincipal(userId::toString)
                                .addRole("USER")
                                .build())
                        .orElse(null));
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        var status = isAdminPath(context) ? 404 : 401;
        return Uni.createFrom().item(new ChallengeData(status, null, null));
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return Set.of();
    }

    private boolean shouldSkipAuthentication(RoutingContext context) {
        if (!"GET".equalsIgnoreCase(context.request().method().name())) {
            return false;
        }

        var path = context.normalizedPath();
        if (path == null) {
            return false;
        }

        return path.matches("^/api/posts/[^/]+/comments$") ||
                path.matches("^/api/posts/[^/]+/comments/[^/]+/replies$");
    }

    private boolean isAdminPath(RoutingContext context) {
        var path = context.normalizedPath();
        return path != null && (path.equals("/api/admin") || path.startsWith("/api/admin/"));
    }

    private boolean isBanExempt(RoutingContext ctx) {
        var path = ctx.normalizedPath();
        return "/api/ban-status".equals(path);
    }
}
