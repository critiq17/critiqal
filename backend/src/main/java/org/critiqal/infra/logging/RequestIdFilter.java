package org.critiqal.infra.logging;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.MDC;

import java.io.IOException;
import java.util.UUID;

@Provider
@Priority(Priorities.USER - 100)
public class RequestIdFilter implements ContainerRequestFilter {

    private static final String MDC_KEY = "requestId";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        MDC.put(MDC_KEY, UUID.randomUUID().toString());
    }
}
