package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import org.broadinstitute.consent.http.models.ontology.DataUseSummary;
import org.broadinstitute.consent.http.models.ontology.DataUseTerm;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.Test;

class DataUseGroupTest {

  /** The Gson the collection summary resources serialize list responses with. */
  private Gson listContextGson() {
    return GsonUtil.gsonBuilderWithAdapters().excludeFieldsWithoutExposeAnnotation().create();
  }

  private DataUseGroup group() {
    return new DataUseGroup(
        List.of(1, 2),
        new DataUseSummary(List.of(new DataUseTerm("GRU", "General research use")), List.of()),
        List.of(new DataUseGroup.GroupDataset(1, "Set A", "DUOS-000001")),
        List.of(new DataUseGroup.GroupVote(7, true, "Alice")));
  }

  @Test
  void testNestedGroupFieldsSurviveExposeFiltering() {
    JsonObject json = JsonParser.parseString(listContextGson().toJson(group())).getAsJsonObject();

    assertEquals(2, json.getAsJsonArray("key").size());
    assertEquals(1, json.getAsJsonArray("key").get(0).getAsInt());
    // The ontology models must survive @Expose filtering too, or the pill renders empty.
    JsonObject primary =
        json.getAsJsonObject("dataUse").getAsJsonArray("primary").get(0).getAsJsonObject();
    assertEquals("GRU", primary.get("code").getAsString());
    assertEquals("General research use", primary.get("description").getAsString());

    JsonObject dataset = json.getAsJsonArray("datasets").get(0).getAsJsonObject();
    assertEquals(1, dataset.get("datasetId").getAsInt());
    assertEquals("Set A", dataset.get("name").getAsString());
    assertEquals("DUOS-000001", dataset.get("datasetIdentifier").getAsString());

    JsonObject vote = json.getAsJsonArray("votes").get(0).getAsJsonObject();
    assertEquals(7, vote.get("userId").getAsInt());
    assertTrue(vote.get("vote").getAsBoolean());
    assertEquals("Alice", vote.get("displayName").getAsString());
  }

  @Test
  void testGroupsAreSerializedOnTheCollectionSummary() {
    DarCollectionSummary summary = new DarCollectionSummary();
    summary.setDarCollectionId(1);
    summary.setDataUseGroups(List.of(group()));

    JsonObject json = JsonParser.parseString(listContextGson().toJson(summary)).getAsJsonObject();

    assertEquals(1, json.getAsJsonArray("dataUseGroups").size());
    assertEquals(
        "GRU",
        json.getAsJsonArray("dataUseGroups")
            .get(0)
            .getAsJsonObject()
            .getAsJsonObject("dataUse")
            .getAsJsonArray("primary")
            .get(0)
            .getAsJsonObject()
            .get("code")
            .getAsString());
  }

  @Test
  void testPendingVoteIsAbsentRatherThanFalse() {
    DataUseGroup pending =
        new DataUseGroup(
            List.of(1), null, List.of(), List.of(new DataUseGroup.GroupVote(7, null, "Alice")));

    JsonObject vote =
        JsonParser.parseString(listContextGson().toJson(pending))
            .getAsJsonObject()
            .getAsJsonArray("votes")
            .get(0)
            .getAsJsonObject();

    // A member who has not voted must not be read as a denial.
    assertTrue(vote.get("vote") == null || vote.get("vote").isJsonNull());
  }
}
