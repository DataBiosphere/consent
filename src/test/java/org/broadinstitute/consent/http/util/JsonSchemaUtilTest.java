package org.broadinstitute.consent.http.util;

import com.networknt.schema.ValidationMessage;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Set;

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
            "phenotypeIndication": "phenotype",
            "species": "species",
            "piName": "PI Name",
            "dataCustodianEmail": ["email@abc.com"],
            "publicVisibility": true,
            "dataSubmitterUserId": 1,
            "nihAnvilUse": "I am not NHGRI funded and do not plan to store data in AnVIL",
            "consentGroups": [{
              "fileTypes": [{
                "fileType": "Arrays",
                "functionalEquivalence": "equivalence",
                "numberOfParticipants": 2
              }],
              "consentGroupName": "name",
              "generalResearchUse": true,
              "dataAccessCommitteeId": 1,
              "url": "https://asdf.com"
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
    Set<ValidationMessage> errors = schemaUtil.validateSchema_v1(instance);
    assertFalse(errors.isEmpty());
  }

  @Test
  public void testIsValidDatasetRegistrationObject_v1_case1() {
    Set<ValidationMessage> errors = schemaUtil.validateSchema_v1(datasetRegistrationInstance);
    assertTrue(errors.isEmpty());
  }

  @Test
  public void testParseDatasetRegistrationObject_v1() {
    DatasetRegistrationSchemaV1 instance = schemaUtil.deserializeDatasetRegistration(datasetRegistrationInstance);
    assertNotNull(instance);
    assertNotNull(instance.getStudyType());
    assertNotNull(instance.getStudyName());
    assertNotNull(instance.getStudyDescription());
    assertFalse(instance.getDataTypes().isEmpty());
    assertNotNull(instance.getPhenotypeIndication());
    assertNotNull(instance.getSpecies());
    assertNotNull(instance.getPiName());
    assertNotNull(instance.getDataSubmitterUserId());
    assertFalse(instance.getDataCustodianEmail().isEmpty());
    assertNotNull(instance.getPublicVisibility());
    assertFalse(instance.getConsentGroups().isEmpty());
    assertFalse(instance.getConsentGroups().get(0).getFileTypes().isEmpty());
    assertNotNull(instance.getConsentGroups().get(0).getDataAccessCommitteeId());
  }

  @Test
  public void testParseDatasetRegistrationObject_v1_valid_date() {
    String instance = """
          {
            "studyType": "Observational",
            "studyName": "name",
            "studyDescription": "description",
            "dataTypes": ["types"],
            "phenotypeIndication": "phenotype",
            "species": "species",
            "piName": "PI Name",
            "dataCustodianEmail": ["email@abc.com"],
            "publicVisibility": true,
            "piInstitution": 1,
            "nihGrantContractNumber": "1234123412341234",
            "dbGaPPhsID": "someId",
            "dataSubmitterUserId": 1,
            "embargoReleaseDate": "1988-10-20",
            "targetDeliveryDate": "1988-10-20",
            "targetPublicReleaseDate": "1988-10-20",
            "nihAnvilUse": "I am NHGRI funded and I have a dbGaP PHS ID already",
            "consentGroups": [{
              "fileTypes": [{
                "fileType": "Arrays",
                "functionalEquivalence": "equivalence",
                "numberOfParticipants": 2
              }],
              "consentGroupName": "name",
              "generalResearchUse": true,
              "dataAccessCommitteeId": 1,
              "url": "https://asdf.com"
            }]
          }
          """;
    Set<ValidationMessage> errors = schemaUtil.validateSchema_v1(instance);
    assertNoErrors(errors);
  }

  @Test
  public void testParseDatasetRegistrationObject_v1_invalid_dates() {
    String instance = """
          {
            "studyType": "Observational",
            "studyName": "name",
            "studyDescription": "description",
            "dataTypes": ["types"],
            "phenotypeIndication": "phenotype",
            "species": "species",
            "piName": "PI Name",
            "dataCustodianEmail": ["email@abc.com"],
            "publicVisibility": true,
            "piInstitution": 1,
            "nihGrantContractNumber": "1234123412341234",
            "gbGaPPhsID": "someId",
            "dataSubmitterUserId": 1,
            "embargoReleaseDate": "asfd-10-20",
            "targetDeliveryDate": "asdf-10-20",
            "targetPublicReleaseDate": "10-10-2000",
            "nihAnvilUse": "I am NHGRI funded and I have a dbGaP PHS ID already",
            "consentGroups": [{
              "fileTypes": [{
                "fileType": "Arrays",
                "functionalEquivalence": "equivalence",
                "numberOfParticipants": 2
              }],
              "consentGroupName": "name",
              "generalResearchUse": true,
              "dataAccessCommitteeId": 1,
              "url": "https://asdf.com"
            }]
          }
          """;
    Set<ValidationMessage> errors = schemaUtil.validateSchema_v1(instance);
    assertFieldHasError(errors, "embargoReleaseDate");
    assertFieldHasError(errors, "targetDeliveryDate");
    assertFieldHasError(errors, "targetPublicReleaseDate");
  }


  private void assertNoErrors(Set<ValidationMessage> errors) {
    assertTrue(String.format("Should be empty, instead was: %s", errors.stream().map(ValidationMessage::toString).toList()), errors.isEmpty());
  }
  private void assertFieldHasError(Set<ValidationMessage> errors, String field) {
    assertTrue(
            String.format("Field %s should have errored", field),
            errors.stream().anyMatch((ValidationMessage s) -> s.getMessage().contains(field)));
  }
}
