package org.broadinstitute.consent.http.exceptions;

import java.io.IOException;

/**
 * A request body passed its size limit while Jersey read it. An {@link IOException} on purpose:
 * MIMEPull closes a multipart message, deleting the temporary files it wrote, only when a read
 * fails with one.
 */
public class RequestTooLargeException extends IOException {
  public RequestTooLargeException(String message) {
    super(message);
  }
}
