package org.critiqal.domain.shared.exception;

import jakarta.ws.rs.core.Response;

public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message, Response.Status.CONFLICT);
    }
}
