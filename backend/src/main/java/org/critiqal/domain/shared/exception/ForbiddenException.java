package org.critiqal.domain.shared.exception;

import jakarta.ws.rs.core.Response;

public class ForbiddenException extends DomainException {

    public ForbiddenException(String message) {
        super(message, Response.Status.FORBIDDEN);
    }
}
