package org.broadinstitute.consent.http.exceptions;

import javax.ws.rs.core.Response;
import javax.ws.rs.ClientErrorException;

public class ConsentConflictException extends ClientErrorException {
 public ConsentConflictException() {
   super(Response.Status.CONFLICT);
 }
}
