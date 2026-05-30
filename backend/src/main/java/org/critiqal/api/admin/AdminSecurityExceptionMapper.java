package org.critiqal.api.admin;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.UnauthorizedException;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.*;

import java.util.Map;

// Quarkus security denials (Forbidden/Unauthorized/AuthFailed) all extend java.lang.SecurityException.
// Hide admin endpoints from unauthorized callers: return 404 instead of 401/403.
@Provider
public class AdminSecurityExceptionMapper implements ExceptionMapper<SecurityException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(SecurityException e) {
        if (uriInfo.getPath().startsWith("api/admin")) {
            return Response.status(404)
                    .entity(Map.of("error", "Not Found"))
                    .type(MediaType.APPLICATION_JSON).build();
        }
        // Non-admin paths keep standard semantics
        boolean unauthorized = e instanceof UnauthorizedException || e instanceof AuthenticationFailedException;
        return Response.status(unauthorized ? 401 : 403)
                .entity(Map.of("error", unauthorized ? "Unauthorized" : "Forbidden"))
                .type(MediaType.APPLICATION_JSON).build();
    }
}
