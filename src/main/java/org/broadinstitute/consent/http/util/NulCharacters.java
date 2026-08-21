package org.broadinstitute.consent.http.util;

/**
 * Removes the one character Postgres will not store. A {@code text} column rejects U+0000 outright
 * and a {@code jsonb} value rejects the six-character escape that denotes it, so both are dropped
 * here, in Java, before a value is bound: a SQL substitution cannot tell a real escape from the
 * literal text of one, and it never sees the values bound alongside the document.
 */
public final class NulCharacters {

  /** What follows the backslash that escapes a U+0000 in a JSON document. */
  private static final String ESCAPE_BODY = "u0000";

  private NulCharacters() {}

  /** Drops every U+0000 from a plain value, such as the name a draft is listed under. */
  public static String stripFrom(String text) {
    if (text == null || text.indexOf('\0') < 0) {
      return text;
    }
    return text.replace("\0", "");
  }

  /**
   * Drops both forms a U+0000 takes in a JSON document: the escape that denotes it, and the
   * character itself should one arrive raw. The parity of a backslash run decides whether its last
   * backslash escapes what follows or is escaped text in its own right, so each run is measured
   * rather than peeked at, and an escaped backslash before the escape keeps both of its own.
   */
  public static String stripFromJsonText(String json) {
    if (json == null || (json.indexOf('\0') < 0 && !json.contains("\\" + ESCAPE_BODY))) {
      return json;
    }
    StringBuilder stripped = new StringBuilder(json.length());
    int index = 0;
    while (index < json.length()) {
      char character = json.charAt(index);
      if (character != '\\') {
        if (character != '\0') {
          stripped.append(character);
        }
        index++;
        continue;
      }
      int runStart = index;
      while (index < json.length() && json.charAt(index) == '\\') {
        index++;
      }
      int run = index - runStart;
      stripped.repeat('\\', run - run % 2);
      if (run % 2 == 1) {
        if (json.startsWith(ESCAPE_BODY, index)) {
          index += ESCAPE_BODY.length();
        } else {
          stripped.append('\\');
        }
      }
    }
    return stripped.toString();
  }
}
