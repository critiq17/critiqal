package org.acme.infra.auth;

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
import org.acme.application.auth.SessionService;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Set;

@ApplicationScoped
public class SessionAuthMechanism implements HttpAuthenticationMechanism {

    private final SessionService sessions;
    private final String cookieName;

    public SessionAuthMechanism(
            SessionService sessions,
            @ConfigProperty(name = "session.cookie.name") String cookieName) {
        this.sessions = sessions;
        this.cookieName = cookieName;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context,
            IdentityProviderManager idm) {
        var cookie = context.request().getCookie(cookieName);
        if (cookie == null)
            return Uni.createFrom().nullItem();
        var sid = cookie.getValue();

        return Uni.createFrom().item(() -> sessions.resolve(sid))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .map(opt -> opt
                        .map(userId -> (SecurityIdentity) QuarkusSecurityIdentity.builder()
                                .setPrincipal(userId::toString)
                                .addRole("USER")
                                .build())
                        .orElse(null));
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        return Uni.createFrom().item(new ChallengeData(401, null, null));
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return Set.of();
    }
}
