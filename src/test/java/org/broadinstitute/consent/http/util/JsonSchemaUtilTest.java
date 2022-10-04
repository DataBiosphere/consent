package org.broadinstitute.consent.http.util;

import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JsonSchemaUtilTest {

  private static JsonSchemaUtil schemaUtil;

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


  @BeforeClass
  public static void setUp() {
    schemaUtil = new JsonSchemaUtil();
  }

  @Test
  public void testIsValidDatasetRegistrationObject_v1_case0() {
    String instance = "{}";
    boolean valid = schemaUtil.isValidSchema_v1(instance);
    assertFalse(valid);
  }

  @Test
  public void testIsValidDatasetRegistrationObject_v1_case1() {
    boolean valid = schemaUtil.isValidSchema_v1(datasetRegistrationInstance);
    assertTrue(valid);
  }

  @Test
  public void testParseDatasetRegistrationObject_v1() {
    DatasetRegistrationSchemaV1 instance = schemaUtil.deserializeDatasetRegistration(datasetRegistrationInstance);
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

  @Test
  public void testParseDatasetRegistrationObject_v1_date_case_1() {
    String instance = """
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
            }],
            "embargoReleaseDate": "2018-11-13"
          }
          """;
    boolean valid = schemaUtil.isValidSchema_v1(instance);
    assertTrue(valid);
  }

  @Test
  public void testParseDatasetRegistrationObject_v1_date_case_2() {
    String instance = """
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
            }],
            "embargoReleaseDate": "asdf-11-13"
          }
          """;
    boolean valid = schemaUtil.isValidSchema_v1(instance);
    assertFalse(valid);
  }  @Test

  public void testParseDatasetRegistrationObject_v1_date_case_3() {
    String instance = """
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
            }],
            "embargoReleaseDate": "12-34-5678"
          }
          """;
    boolean valid = schemaUtil.isValidSchema_v1(instance);
    assertFalse(valid);
  }
}
