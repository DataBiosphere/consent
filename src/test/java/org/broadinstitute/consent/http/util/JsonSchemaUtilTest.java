package org.broadinstitute.consent.http.util;

import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JsonSchemaUtilTest {

  private final String datasetRegistrationInstance = """
          {
            "studyType": "Observational",
            "studyName": "name",
            "studyDescription": "description",
            "dataTypes": ["types"],
            "fileTypes": [{
              "fileType": "Arrays",
              "functionalEquivalence": "equivalence",
              "numberOfParticipants": 2
            }],
            "phenotypeIndication": "phenotype",
            "species": "species",
            "piName": "PI Name",
            "dataSubmitterUserId": 1,
            "dataCustodianEmail": ["email@abc.com"],
            "publicVisibility": true,
            "dataAccessCommitteeId": 1,
            "consentGroups": [{
              "consentGroupName": "name",
              "generalResearchUse": true
            }]
          }
          """;

  @Test
  public void testIsValidDataSubmitterObject_v1_case0() {
    String instance = "{}";
    JsonSchemaUtil util = new JsonSchemaUtil();
    boolean valid = util.isValidSchema_v1(instance);
    assertFalse(valid);
  }

  @Test
  public void testIsValidDataSubmitterObject_v1_case1() {
    JsonSchemaUtil util = new JsonSchemaUtil();
    boolean valid = util.isValidSchema_v1(datasetRegistrationInstance);
    assertTrue(valid);
  }

  @Test
  public void testParseDataSubmitterObject_v1() {
    JsonSchemaUtil util = new JsonSchemaUtil();
    DatasetRegistrationSchemaV1 instance = util.deserialize(datasetRegistrationInstance);
    assertNotNull(instance);
    assertNotNull(instance.getStudyType());
    assertNotNull(instance.getStudyName());
    assertNotNull(instance.getStudyDescription());
    assertFalse(instance.getDataTypes().isEmpty());
    assertFalse(instance.getFileTypes().isEmpty());
    assertNotNull(instance.getPhenotypeIndication());
    assertNotNull(instance.getSpecies());
    assertNotNull(instance.getPiName());
    assertNotNull(instance.getDataSubmitterUserId());
    assertFalse(instance.getDataCustodianEmail().isEmpty());
    assertNotNull(instance.getPublicVisibility());
    assertNotNull(instance.getDataAccessCommitteeId());
    assertFalse(instance.getConsentGroups().isEmpty());
  }
}
