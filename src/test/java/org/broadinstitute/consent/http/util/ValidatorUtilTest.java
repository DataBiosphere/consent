package org.broadinstitute.consent.http.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ValidatorUtilTest {

  @Test
  void testIsInvalidDateWithValidDate() {
    assertFalse(ValidatorUtil.isInvalidDate("2024-06-01"));
  }

  @Test
  void testIsInvalidDateWithInvalidDate() {
    assertTrue(ValidatorUtil.isInvalidDate("2024-13-01")); // invalid month
    assertTrue(ValidatorUtil.isInvalidDate("not-a-date"));
    assertTrue(ValidatorUtil.isInvalidDate(""));
    assertTrue(ValidatorUtil.isInvalidDate(null));
  }

  @Test
  void testIsInvalidURIWithValidURI() {
    assertFalse(ValidatorUtil.isInvalidURI("https://example.com"));
    assertFalse(ValidatorUtil.isInvalidURI("http://example.com/path"));
  }

  @Test
  void testIsInvalidURIWithInvalidURI() {
    assertTrue(ValidatorUtil.isInvalidURI("ht!tp://[invalid-uri]"));
    assertTrue(ValidatorUtil.isInvalidURI("example.com")); // missing scheme
    assertTrue(ValidatorUtil.isInvalidURI("http:///path")); // missing host
    assertTrue(ValidatorUtil.isInvalidURI(""));
    assertTrue(ValidatorUtil.isInvalidURI(null));
  }
}
