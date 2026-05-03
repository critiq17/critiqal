package org.critiqal.domain.shared.exception;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.critiqal.infra.telegram.AlertService;
import org.jboss.logging.Logger;

import java.util.Map;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<DomainException> {

    @Override
    public Response toResponse(DomainException e) {
        return Response.status(e.status())
                .entity(Map.of("error", e.getMessage()))
                .build();
    }
}

@Provider
class UnhandledExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger log = Logger.getLogger(UnhandledExceptionMapper.class);

    @Inject
    AlertService alertService;

    @Override
    public Response toResponse(Exception e) {
        log.error("Unhandled exception", e);
        alertService.error("Unhandled server error", e);
        return Response.serverError()
                .entity(Map.of("error", "Internal server error"))
                .build();
    }
}
