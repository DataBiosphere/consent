package org.broadinstitute.consent.http.service.showcase;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The continuation cursor of the {@code all} mode. It is opaque to callers: URL-safe Base64 of a
 * version, the filters it was made for and the last alias of the page. A cursor made for other
 * filters, or in another form, is refused rather than read as a position.
 *
 * <p>A caller can still write a valid cursor for any alias. That only starts a page there, which
 * the caller could reach by paging anyway, so the cursor needs no signature.
 */
final class ShowcaseCursor {

  /** The {@code all} mode has no filters yet, so every cursor is bound to the whole catalog. */
  private static final String ALL_FILTERS = "all";

  private static final String VERSION = "v1";
  private static final Pattern FORM =
      Pattern.compile("^" + VERSION + ":" + ALL_FILTERS + ":(0|[1-9]\\d{0,9})$");

  private ShowcaseCursor() {}

  static String encode(int lastAlias) {
    String plain = "%s:%s:%d".formatted(VERSION, ALL_FILTERS, lastAlias);
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(plain.getBytes(StandardCharsets.US_ASCII));
  }

  /**
   * @return the last alias of the previous page
   * @throws ShowcaseLookupException when the cursor is not one that {@link #encode} makes
   */
  static int decode(String cursor) {
    try {
      String plain = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.US_ASCII);
      Matcher matcher = FORM.matcher(plain);
      if (matcher.matches() && encode(Integer.parseInt(matcher.group(1))).equals(cursor)) {
        return Integer.parseInt(matcher.group(1));
      }
    } catch (IllegalArgumentException e) {
      // Reported below. NumberFormatException is an IllegalArgumentException.
    }
    throw new ShowcaseLookupException("Field \"cursor\" is not a valid cursor");
  }
}
