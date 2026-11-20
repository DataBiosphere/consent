package org.broadinstitute.consent.http.cloudstore;

import jakarta.ws.rs.BadRequestException;
import java.math.BigDecimal;

/** An upload passed the configured size limit. The client's error, returned as a 400. */
public class UploadTooLargeException extends BadRequestException {

  public UploadTooLargeException(long maxUploadBytes) {
    super(
        "File size is invalid. Max size is: %s MB"
            .formatted(BigDecimal.valueOf(maxUploadBytes, 6).stripTrailingZeros().toPlainString()));
  }
}
