package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1Builder;
import org.junit.jupiter.api.Test;

class DatasetRegistrationSchemaV1BuilderTest {

  @Test
  void testBuildEmptySchema() {
    DatasetRegistrationSchemaV1Builder builder = new DatasetRegistrationSchemaV1Builder();
    Study study = new Study();
    DatasetRegistrationSchemaV1 schemaV1 = builder.build(study, List.of());
    assertNotNull(schemaV1);
  }

}
