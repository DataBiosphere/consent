package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.MatchAlgorithm;
import org.broadinstitute.consent.http.models.matching.DataUseMatchResultType;
import org.junit.jupiter.api.Test;

class MatchTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void datasetIdIsNotSerialized() throws Exception {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(42);
    dataset.setAlias(7);
    Match match =
        Match.matchSuccess(
            dataset, "purpose", DataUseMatchResultType.APPROVE, MatchAlgorithm.V5, List.of());

    ObjectNode json = (ObjectNode) mapper.readTree(mapper.writeValueAsString(match));

    // dataset_id is internal identity: responses carry the dataset as its public identifier only.
    assertFalse(json.has("datasetId"));
    assertTrue(json.has("consent"));
    assertEquals(dataset.getDatasetIdentifier(), json.get("consent").asText());
    assertEquals(42, match.getDatasetId());
  }
}
