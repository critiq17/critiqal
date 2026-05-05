package org.critiqal.domain.shared.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.critiqal.domain.alert.service.AlertService;
import org.jboss.logging.Logger;

import java.util.Map;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<DomainException> {

    @Override
    public Response toResponse(DomainException e) {
        return error(e.status(), e.getMessage());
    }

    static Response error(Response.StatusType status, String message) {
        var errorMessage = message == null || message.isBlank()
                ? status.getReasonPhrase()
                : message;
        return Response.status(status)
                .entity(Map.of("error", errorMessage))
                .build();
    }
}

@Provider
class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException e) {
        return GlobalExceptionMapper.error(Response.Status.BAD_REQUEST, e.getMessage());
    }
}

@Provider
class UnhandledExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger log = Logger.getLogger(UnhandledExceptionMapper.class);
    private final AlertService alertService;

    public UnhandledExceptionMapper(AlertService alertService) {
        this.alertService = alertService;
    }

    @Override
    public Response toResponse(Exception e) {
        log.error("Unhandled exception", e);
        alertService.error("Unhandled server error", e);
        return GlobalExceptionMapper.error(Response.Status.INTERNAL_SERVER_ERROR, "Internal server error");
    }
}
