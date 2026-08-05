package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class MatchTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void testDatasetIdIsInternalOnly() {
    Match match = new Match();
    match.setDatasetId(123);
    match.setConsent("DUOS-000123");

    JsonNode json = mapper.valueToTree(match);

    assertFalse(json.has("datasetId"));
    assertEquals("DUOS-000123", json.get("consent").asText());
  }
}
