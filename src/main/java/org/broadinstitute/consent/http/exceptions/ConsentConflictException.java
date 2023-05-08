package org.broadinstitute.consent.http.exceptions;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

public class ConsentConflictException extends ClientErrorException {
    public ConsentConflictException() {
        super(Response.Status.CONFLICT);
    }

    public ConsentConflictException(String message) {
        super(message, Response.Status.CONFLICT);
    }
}
