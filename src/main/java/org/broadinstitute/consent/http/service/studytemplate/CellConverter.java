package org.broadinstitute.consent.http.service.studytemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Function;

/**
 * Converts one non-empty template cell into the wire value for its field. Rejection messages name
 * the CSV field rather than the wire label, so a producer can find the offending cell.
 */
@FunctionalInterface
interface CellConverter {

  /** The converted value, or the reason the cell was rejected. */
  record CellValue(Object value, String errorMessage) {

    static CellValue of(Object value) {
      return new CellValue(value, null);
    }

    static CellValue rejected(String errorMessage) {
      return new CellValue(null, errorMessage);
    }

    boolean rejected() {
      return errorMessage != null;
    }
  }

  CellValue convert(String field, String value);

  /** Text and dates reach the wire verbatim; the registration validator owns the date rule. */
  CellConverter TEXT = (_, value) -> CellValue.of(value);

  CellConverter BOOLEAN =
      (field, value) ->
          switch (value) {
            case "true" -> CellValue.of(Boolean.TRUE);
            case "false" -> CellValue.of(Boolean.FALSE);
            default -> CellValue.rejected(field + " must be true or false");
          };

  CellConverter INTEGER =
      (field, value) -> {
        try {
          return CellValue.of(Integer.valueOf(value));
        } catch (NumberFormatException _) {
          return CellValue.rejected(field + " must be a whole number");
        }
      };

  CellConverter HTTP_URI =
      (field, value) -> {
        String rejection = field + " must be an absolute http or https URL";
        try {
          URI uri = new URI(value);
          boolean absoluteHttp =
              uri.isAbsolute()
                  && (uri.getScheme().equals("http") || uri.getScheme().equals("https"))
                  && uri.getHost() != null;
          return absoluteHttp ? CellValue.of(uri) : CellValue.rejected(rejection);
        } catch (URISyntaxException _) {
          return CellValue.rejected(rejection);
        }
      };

  /** Enum matching is case-sensitive and uses the wire value, per the v1 contract. */
  static CellConverter enumOf(Function<String, ?> fromValue) {
    return (field, value) -> {
      try {
        return CellValue.of(fromValue.apply(value));
      } catch (IllegalArgumentException _) {
        return CellValue.rejected("Unknown " + field + " value: " + value);
      }
    };
  }
}
