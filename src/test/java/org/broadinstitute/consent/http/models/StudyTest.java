package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.util.Set;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.Test;

/**
 * A study lists its datasets by id and does not embed them, so the Study and Dataset payloads no
 * longer reference each other. These pin that contract at the serializer the API responds with.
 */
class StudyTest {

  @Test
  void aSerializedStudyListsDatasetIdsAndNoDatasets() {
    Study study = new Study();
    study.setStudyId(1);
    study.addDatasetIds(Set.of(10, 11));

    JsonObject json = GsonUtil.getInstance().toJsonTree(study).getAsJsonObject();

    assertFalse(json.has("datasets"));
    assertTrue(json.has("datasetIds"));
    assertEquals(2, json.getAsJsonArray("datasetIds").size());
  }

  @Test
  void aDatasetsStudyDoesNotEmbedTheDatasetAgain() {
    Study study = new Study();
    study.setStudyId(1);
    study.addDatasetIds(Set.of(10));
    Dataset dataset = new Dataset();
    dataset.setDatasetId(10);
    dataset.setStudyId(study.getStudyId());
    dataset.setStudy(study);

    JsonObject json = GsonUtil.getInstance().toJsonTree(dataset).getAsJsonObject();

    JsonObject nestedStudy = json.getAsJsonObject("study");
    assertEquals(1, nestedStudy.get("studyId").getAsInt());
    assertFalse(nestedStudy.has("datasets"));
  }
}
