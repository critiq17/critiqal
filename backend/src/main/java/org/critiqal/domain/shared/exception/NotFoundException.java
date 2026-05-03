package org.critiqal.domain.shared.exception;

import jakarta.ws.rs.core.Response;

public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(message, Response.Status.NOT_FOUND);
    }
}
