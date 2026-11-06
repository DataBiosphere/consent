package org.broadinstitute.consent.http.models.dataset_registration_v1.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.Test;

class SchemaFromStudyTest {

  @Test
  void testTheRegistrationCarriesNoStoredIdentifier() {
    Study study = new Study();
    study.setStudyId(1);
    study.addProperty(
        new StudyProperty(
            "models",
            GsonUtil.getInstance()
                .toJsonTree(List.of(Map.of("modelId", 45, "duosIdentifier", "DUOS-M-000045"))),
            PropertyType.Json));
    study.addProperty(
        new StudyProperty(
            "assets",
            GsonUtil.getInstance()
                .toJsonTree(
                    Map.of(
                        "biospecimens",
                        List.of(Map.of("biospecimenId", 4, "duosIdentifier", "DUOS-B-000004")))),
            PropertyType.Json));

    DatasetRegistrationSchemaV1 schema = new SchemaFromStudy().build(study);

    assertEquals(List.of(Map.of("modelId", 45L)), schema.getModels());
    assertEquals(List.of(Map.of("biospecimenId", 4L)), schema.getBiospecimens());
    assertEquals(List.of(Map.of("modelId", 45L)), schema.getAssets().get("models"));
    assertEquals(List.of(Map.of("biospecimenId", 4L)), schema.getAssets().get("biospecimens"));
  }
}
