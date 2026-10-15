package org.broadinstitute.consent.http.models.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DatasetMailDTOTest {

  @ParameterizedTest
  @ValueSource(
      strings = {"https://example.org/data", "HTTP://example.org/data", " https://example.org "})
  void testHasWebDataLocationUrl(String url) {
    assertTrue(new DatasetMailDTO(1, "name", "DUOS-000001", url).hasWebDataLocationUrl());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {
        " ",
        "http://",
        "javascript:alert(1)",
        "gs://bucket/object",
        "example.org/data",
        "ftp://example.org/data",
        "http://localhost:8080/data"
      })
  void testHasWebDataLocationUrl_NotWebUrl(String url) {
    assertFalse(new DatasetMailDTO(1, "name", "DUOS-000001", url).hasWebDataLocationUrl());
  }

  @Test
  void testDataLocationUrlIsStripped() {
    assertEquals(
        "https://example.org/data",
        new DatasetMailDTO(1, "name", "DUOS-000001", "\u3000https://example.org/data\n")
            .dataLocationUrl());
  }
}
