package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.enumeration.ShowcasePublicField;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.Test;

class ShowcaseDatasetSummaryTest {

  /** Responses must use the null-serializing Gson so that every key is present. */
  private final Gson gson = GsonUtil.getNullSerializingInstance();

  @Test
  void testSerializedKeysEqualAllowlist() {
    ShowcaseDatasetSummary summary =
        new ShowcaseDatasetSummary(null, null, null, null, null, null, null, null, null);
    JsonObject json = gson.toJsonTree(summary).getAsJsonObject();

    assertEquals(ShowcasePublicField.jsonNames(), List.copyOf(json.keySet()));
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
