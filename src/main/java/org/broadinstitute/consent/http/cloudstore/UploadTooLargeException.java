package org.broadinstitute.consent.http.cloudstore;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;

/**
 * An upload passed the configured size limit. The client's error, returned as a 413 like a request
 * past the request size limit (API_GUIDELINES).
 */
public class UploadTooLargeException extends ClientErrorException {

  public UploadTooLargeException(long maxUploadBytes) {
    super(
        "File size is invalid. Max size is: %s MB"
            .formatted(BigDecimal.valueOf(maxUploadBytes, 6).stripTrailingZeros().toPlainString()),
        Response.Status.REQUEST_ENTITY_TOO_LARGE);
  }
}
