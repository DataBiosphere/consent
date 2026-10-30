package org.broadinstitute.consent.http.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ContentLengthTest {

  @ParameterizedTest
  @ValueSource(strings = {"0", "65536", " 65537 ", "4294967296"})
  void testDeclaredLength(String header) {
    assertEquals(Long.parseLong(header.trim()), ContentLength.declared(header));
  }

  /** Unknown lengths, so the bounded read decides. */
  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "  ", "abc", "1.5", "99999999999999999999"})
  void testUnknownLength(String header) {
    assertEquals(-1, ContentLength.declared(header));
  }
}
