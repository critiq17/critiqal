package org.critiqal.infra.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

@Provider
public class SecurityHeadersFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) throws IOException {
        res.getHeaders().putSingle("X-Content-Type-Options", "nosniff");
        res.getHeaders().putSingle("Referrer-Policy", "strict-origin-when-cross-origin");
        res.getHeaders().putSingle("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        res.getHeaders().putSingle("X-Frame-Options", "DENY");
    }
}
