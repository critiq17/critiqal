package org.critiqal.infra.security;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Provider
@Priority(Priorities.AUTHENTICATION + 1)
public class CsrfFilter implements ContainerRequestFilter {

    private static final Set<String> STATE_CHANGING_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");
    private static final Set<String> ALWAYS_ALLOWED_ORIGINS = Set.of(
            "https://web.telegram.org"
    );

    private final Set<String> allowedOrigins;

    @Inject
    CsrfFilter(@ConfigProperty(name = "csrf.allowed-origins",
                               defaultValue = "http://localhost:5173") String rawOrigins) {
        var configured = Arrays.stream(rawOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty());
        this.allowedOrigins = Stream.concat(configured, ALWAYS_ALLOWED_ORIGINS.stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void filter(ContainerRequestContext ctx) {
        if (!STATE_CHANGING_METHODS.contains(ctx.getMethod())) {
            return;
        }

        String origin = ctx.getHeaderString("Origin");

        if (origin == null) {
            // Fall back to Referer — extract origin (scheme + host + port)
            String referer = ctx.getHeaderString("Referer");
            if (referer != null) {
                origin = extractOrigin(referer);
            }
        }

        // No Origin and no Referer — allow (TMA / direct API tools)
        if (origin == null) {
            return;
        }

        if (!allowedOrigins.contains(origin)) {
            ctx.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\":\"CSRF check failed\"}")
                    .type("application/json")
                    .build());
        }
    }

    private String extractOrigin(String referer) {
        try {
            var uri = URI.create(referer);
            var sb = new StringBuilder(uri.getScheme()).append("://").append(uri.getHost());
            if (uri.getPort() != -1) {
                sb.append(":").append(uri.getPort());
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
