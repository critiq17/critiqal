package org.critiqal.infra.auth.admin;

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
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Set;

@ApplicationScoped
public class AdminAuthMechanism implements HttpAuthenticationMechanism {

    private final AdminSessionService sessions;
    private final AdminAllowList allowList;
    private final String cookieName;

    public AdminAuthMechanism(AdminSessionService sessions, AdminAllowList allowList,
                              @ConfigProperty(name = "admin.session.cookie.name") String cookieName) {
        this.sessions = sessions;
        this.allowList = allowList;
        this.cookieName = cookieName;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext ctx, IdentityProviderManager idm) {
        if (!isAdminPath(ctx)) return Uni.createFrom().nullItem();
        var cookie = ctx.request().getCookie(cookieName);
        if (cookie == null) return Uni.createFrom().nullItem();
        var sid = cookie.getValue();
        return Uni.createFrom().item(() -> sessions.resolve(sid))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .map(opt -> opt
                        .filter(allowList::contains)
                        .map(uid -> (SecurityIdentity) QuarkusSecurityIdentity.builder()
                                .setPrincipal(uid::toString)
                                .addRole("USER")
                                .addRole("ADMIN")
                                .build())
                        .orElse(null));
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext ctx) {
        var status = isAdminPath(ctx) ? 404 : 401;
        return Uni.createFrom().item(new ChallengeData(status, null, null));
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return Set.of();
    }

    private boolean isAdminPath(RoutingContext ctx) {
        var path = ctx.normalizedPath();
        return path != null && (path.equals("/api/admin") || path.startsWith("/api/admin/"));
    }
}
