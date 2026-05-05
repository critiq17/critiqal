package org.critiqal.domain.shared.exception;

import jakarta.ws.rs.core.Response;

public class DomainException extends RuntimeException {

    private final Response.StatusType status;

    public DomainException(String message) {
        this(message, Response.Status.BAD_REQUEST);
    }

    protected DomainException(String message, Response.StatusType status) {
        super(message);
        this.status = status;
    }

    public Response.StatusType status() {
        return status;
    }
}
