package org.broadinstitute.consent.http.service.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ShowcaseLookupExceptionTest {

  private static final int MAX = ShowcaseLookupException.MAX_ECHOED_LENGTH;

  @Test
  void testShortValueIsUnchanged() {
    String value = "x".repeat(MAX);

    assertEquals(value, ShowcaseLookupException.echoed(value));
  }

  @Test
  void testLongValueIsCut() {
    assertEquals("x".repeat(MAX) + "…", ShowcaseLookupException.echoed("x".repeat(60 * 1024)));
  }

  /** The cut counts code points, so it never splits a surrogate pair. */
  @Test
  void testCutKeepsSurrogatePairs() {
    String emoji = "😀";

    assertEquals(emoji.repeat(MAX) + "…", ShowcaseLookupException.echoed(emoji.repeat(MAX + 1)));
  }
}
