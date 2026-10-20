package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jackson.Jackson;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.enumeration.ShowcasePublicField;
import org.junit.jupiter.api.Test;

class ShowcaseDatasetSummaryTest {

  private final ObjectMapper mapper = Jackson.newObjectMapper();

  @Test
  void testSerializedKeysEqualAllowlist() throws Exception {
    ShowcaseDatasetSummary summary =
        new ShowcaseDatasetSummary(null, null, null, null, null, null, null, null, null);
    JsonNode json = mapper.valueToTree(summary);
    List<String> keys = new ArrayList<>();
    json.fieldNames().forEachRemaining(keys::add);

    assertEquals(ShowcasePublicField.jsonNames(), keys);
  }

  @Test
  void testRecordComponentsEqualAllowlist() {
    List<String> components =
        Stream.of(ShowcaseDatasetSummary.class.getRecordComponents())
            .map(java.lang.reflect.RecordComponent::getName)
            .toList();

    assertEquals(ShowcasePublicField.jsonNames(), components);
  }

  @Test
  void testPropertyAllowlists() {
    assertEquals(
        List.of("numberOfParticipants", "url"), ShowcasePublicField.datasetPropertyAllowlist());
    assertEquals(List.of("species"), ShowcasePublicField.studyPropertyAllowlist());
  }
}
