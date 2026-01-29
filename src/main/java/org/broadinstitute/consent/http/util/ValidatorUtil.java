package org.broadinstitute.consent.http.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ValidatorUtil {

  public static boolean isInvalidDate(String value) {
    if (value == null || value.isEmpty()) {
      return true;
    }
    try {
      LocalDate.parse(value); // expects ISO_LOCAL_DATE (YYYY-MM-DD)
      return false;
    } catch (DateTimeParseException e) {
      return true;
    }
  }

  public static boolean isInvalidURI(String value) {
    if (value == null || value.isEmpty()) {
      return true;
    }
    try {
      URI uri = new URI(value);
      // Require a scheme and host for stricter validation
      return uri.getScheme() == null || uri.getHost() == null;
    } catch (URISyntaxException e) {
      return true;
    }
  }
}
