package org.broadinstitute.consent.http.exceptions;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

public class ConsentConflictException extends ClientErrorException {
    public ConsentConflictException() {
        super(Response.Status.CONFLICT);
    }

    public ConsentConflictException(String message) {
        super(message, Response.Status.CONFLICT);
    }
}
