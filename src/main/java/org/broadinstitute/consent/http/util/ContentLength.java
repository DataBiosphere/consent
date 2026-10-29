package org.broadinstitute.consent.http.util;

/**
 * Reads a {@code Content-Length} header for a size cap. It reads the value as a long rather than
 * through {@code getLength()}, whose int parse turns the header of a multi-gigabyte body — the one
 * a cap most needs to catch — into an unknown length.
 */
public final class ContentLength {

  private ContentLength() {}

  /**
   * The declared length, or -1 when the header is missing, blank or not a number. A caller that
   * gets -1 must still bound the read, because a chunked body has no declared length.
   */
  public static long declared(String header) {
    if (header == null || header.isBlank()) {
      return -1;
    }
    try {
      return Long.parseLong(header.trim());
    } catch (NumberFormatException e) {
      return -1;
    }
  }
}
