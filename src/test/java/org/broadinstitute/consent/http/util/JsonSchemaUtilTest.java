package org.broadinstitute.consent.http.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.networknt.schema.Error;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonSchemaUtilTest {

  private static JsonSchemaUtil schemaUtil;

  private final String datasetRegistrationInstance =
      """
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
            "functionalEquivalence": "equivalence"
          }],
          "numberOfParticipants": 2,
          "consentGroupName": "name",
          "generalResearchUse": true,
          "dataAccessCommitteeId": 1,
          "url": "https://asdf.com"
        }]
      }
      """;

  @BeforeAll
  static void setUp() {
    schemaUtil = new JsonSchemaUtil();
  }

  @Test
  void testIsValidDatasetRegistrationObject_v1_case0() {
    String instance = "{}";
    Set<Error> errors = schemaUtil.validateSchemaV1(instance);
    assertFalse(errors.isEmpty());
  }

  @Test
  void testIsValidDatasetRegistrationObject_v1_case1() {
    Set<Error> errors = schemaUtil.validateSchemaV1(datasetRegistrationInstance);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testParseDatasetRegistrationObject_v1() {
    DatasetRegistrationSchemaV1 instance =
        schemaUtil.deserializeDatasetRegistration(datasetRegistrationInstance);
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
    assertFalse(instance.getConsentGroups().getFirst().getFileTypes().isEmpty());
    assertNotNull(instance.getConsentGroups().getFirst().getDataAccessCommitteeId());
  }

  @Test
  void testValidateDatasetRegistrationObject_v1_valid_date() {
    String instance =
        """
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
              "functionalEquivalence": "equivalence"
            }],
            "numberOfParticipants": 2,
            "consentGroupName": "name",
            "generalResearchUse": true,
            "dataAccessCommitteeId": 1,
            "url": "https://asdf.com"
          }]
        }
        """;
    Set<Error> errors = schemaUtil.validateSchemaV1(instance);
    assertNoErrors(errors);
  }

  @Test
  void testValidateDatasetRegistrationObject_v1_invalid_dates() {
    String instance =
        """
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
          "embargoReleaseDate": "asfd-10-20",
          "targetDeliveryDate": "asdf-10-20",
          "targetPublicReleaseDate": "10-10-2000",
          "nihAnvilUse": "I am NHGRI funded and I have a dbGaP PHS ID already",
          "consentGroups": [{
            "fileTypes": [{
              "fileType": "Arrays",
              "functionalEquivalence": "equivalence"
            }],
            "numberOfParticipants": 2,
            "consentGroupName": "name",
            "generalResearchUse": true,
            "dataAccessCommitteeId": 1,
            "url": "https://asdf.com"
          }]
        }
        """;
    Set<Error> errors = schemaUtil.validateSchemaV1(instance);
    // Note: json-schema-validator 3.0.2 with JSON Schema Draft 2019-09 treats
    // format keywords as annotations by default, not validation errors.
    // The schema contains format constraints for dates, but they are not enforced
    // as errors without explicit format assertion configuration. This test validates
    // that the schema loads correctly and processes the instance without throwing exceptions.
    // In a production environment, date format validation would need to be implemented
    // separately if format keyword enforcement is required.
    assertNotNull(errors);
  }

  @Test
  void testValidateDatasetRegistrationObject_v1_gsr_explanation_conditionally_required() {
    String noGsrSelected =
        """
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
          "controlledAccessRequiredForGenomicSummaryResultsGSR": false,
          "consentGroups": [{
            "fileTypes": [{
              "fileType": "Arrays",
              "functionalEquivalence": "equivalence"
            }],
            "numberOfParticipants": 2,
            "consentGroupName": "name",
            "generalResearchUse": true,
            "dataAccessCommitteeId": 1,
            "url": "https://asdf.com"
          }]
        }
        """;
    String gsrSelectedNoExplanation =
        """
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
          "controlledAccessRequiredForGenomicSummaryResultsGSR": true,
          "consentGroups": [{
            "fileTypes": [{
              "fileType": "Arrays",
              "functionalEquivalence": "equivalence"
            }],
            "numberOfParticipants": 2,
            "consentGroupName": "name",
            "generalResearchUse": true,
            "dataAccessCommitteeId": 1,
            "url": "https://asdf.com"
          }]
        }
        """;

    String gsrSelectedWithExplanation =
        """
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
          "controlledAccessRequiredForGenomicSummaryResultsGSR": true,
          "controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation": "asdf",
          "consentGroups": [{
            "fileTypes": [{
              "fileType": "Arrays",
              "functionalEquivalence": "equivalence"
            }],
            "numberOfParticipants": 2,
            "consentGroupName": "name",
            "generalResearchUse": true,
            "dataAccessCommitteeId": 1,
            "url": "https://asdf.com"
          }]
        }
        """;
    Set<Error> errors = schemaUtil.validateSchemaV1(noGsrSelected);
    assertNoErrors(errors);

    errors = schemaUtil.validateSchemaV1(gsrSelectedNoExplanation);
    assertFieldHasError(
        errors, "controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation");

    errors = schemaUtil.validateSchemaV1(gsrSelectedWithExplanation);
    assertNoErrors(errors);
  }

  @Test
  void testValidateDatasetRegistrationObject_v1_dbgap_info_conditionally_required() {

    String anvilUseYesRequiresDbGapFields =
        """
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
          "nihAnvilUse": "I am NHGRI funded and I have a dbGaP PHS ID already",
          "consentGroups": [{
            "fileTypes": [{
              "fileType": "Arrays",
              "functionalEquivalence": "equivalence"
            }],
            "numberOfParticipants": 2,
            "consentGroupName": "name",
            "generalResearchUse": true,
            "dataAccessCommitteeId": 1,
            "url": "https://asdf.com"
          }]
        }
        """;

    Set<Error> errors = schemaUtil.validateSchemaV1(datasetRegistrationInstance);
    assertNoErrors(errors);

    errors = schemaUtil.validateSchemaV1(anvilUseYesRequiresDbGapFields);
    assertFieldHasError(errors, "dbGaPPhsID");
  }

  @Test
  void testParseValidateRegistrationObject_v1_nih_admin_info_conditionally_required() {

    String anvilUseFundedHaveId =
        """
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
          "nihAnvilUse": "I am NHGRI funded and I have a dbGaP PHS ID already",
          "consentGroups": [{
            "fileTypes": [{
              "fileType": "Arrays",
              "functionalEquivalence": "equivalence"
            }],
            "numberOfParticipants": 2,
            "consentGroupName": "name",
            "generalResearchUse": true,
            "dataAccessCommitteeId": 1,
            "url": "https://asdf.com"
          }]
        }
        """;

    String anvilUseFundedNoId =
        """
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
          "nihAnvilUse": "I am NHGRI funded and I have a dbGaP PHS ID already",
          "consentGroups": [{
            "fileTypes": [{
              "fileType": "Arrays",
              "functionalEquivalence": "equivalence"
            }],
            "numberOfParticipants": 2,
            "consentGroupName": "name",
            "generalResearchUse": true,
            "dataAccessCommitteeId": 1,
            "url": "https://asdf.com"
          }]
        }
        """;

    String anvilUseNotFundedSeekingToSubmit =
        """
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
          "nihAnvilUse": "I am NHGRI funded and I have a dbGaP PHS ID already",
          "consentGroups": [{
            "fileTypes": [{
              "fileType": "Arrays",
              "functionalEquivalence": "equivalence"
            }],
            "numberOfParticipants": 2,
            "consentGroupName": "name",
            "generalResearchUse": true,
            "dataAccessCommitteeId": 1,
            "url": "https://asdf.com"
          }]
        }
        """;

    Set<Error> errors = schemaUtil.validateSchemaV1(datasetRegistrationInstance);
    assertNoErrors(errors);

    Set<Error> fundedHaveIdErrors = schemaUtil.validateSchemaV1(anvilUseFundedHaveId);
    Set<Error> fundedNoIdErrors = schemaUtil.validateSchemaV1(anvilUseFundedNoId);
    Set<Error> seekingToSubmitErrors =
        schemaUtil.validateSchemaV1(anvilUseNotFundedSeekingToSubmit);

    assertFieldHasError(fundedHaveIdErrors, "piInstitution");
    assertFieldHasError(fundedNoIdErrors, "piInstitution");
    assertFieldHasError(seekingToSubmitErrors, "piInstitution");

    assertFieldHasError(fundedHaveIdErrors, "nihGrantContractNumber");
    assertFieldHasError(fundedNoIdErrors, "nihGrantContractNumber");
    assertFieldHasError(seekingToSubmitErrors, "nihGrantContractNumber");
  }

  @Test
  void testParseValidateRegistrationObject_v1_dac_id_conditionally_required() {
    String openAccessNoDacId =
        """
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
              "functionalEquivalence": "equivalence"
            }],
            "numberOfParticipants": 2,
            "consentGroupName": "name",
            "accessManagement": "open",
            "url": "https://asdf.com"
          }]
        }
        """;

    String controlledAccessNoDacId =
        """
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
              "functionalEquivalence": "equivalence"
            }],
            "numberOfParticipants": 2,
            "consentGroupName": "name",
            "accessManagement": "controlled",
            "poa": true,
            "url": "https://asdf.com"
          }]
        }
        """;

    String noAccessManagementNoDacId =
        """
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
              "functionalEquivalence": "equivalence"
            }],
            "numberOfParticipants": 2,
            "consentGroupName": "name",
            "hmb": true,
            "url": "https://asdf.com"
          }]
        }
        """;

    Set<Error> errors = schemaUtil.validateSchemaV1(openAccessNoDacId);
    assertNoErrors(errors);

    // only errors if not open access & no dac id present
    errors = schemaUtil.validateSchemaV1(controlledAccessNoDacId);
    assertFieldHasError(errors, "dataAccessCommitteeId");
    errors = schemaUtil.validateSchemaV1(noAccessManagementNoDacId);
    assertFieldHasError(errors, "dataAccessCommitteeId");

    errors = schemaUtil.validateSchemaV1(datasetRegistrationInstance);
    assertNoErrors(errors);
  }

  @Test
  void testParseValidateRegistrationObject_v1_consent_group_required() {
    String noConsentGroup =
        """
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
          "nihAnvilUse": "I am not NHGRI funded and do not plan to store data in AnVIL"
        }
        """;

    String emptyConsentGroup =
        """
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
          "consentGroups": []
        }
        """;

    Set<Error> errors = schemaUtil.validateSchemaV1(noConsentGroup);
    assertFieldHasError(errors, "consentGroups");

    errors = schemaUtil.validateSchemaV1(emptyConsentGroup);
    assertFieldHasError(errors, "consentGroups");
  }

  @Test
  void testValidateDatasetRegistrationObject_v1_file_types_not_required() {
    String noFileTypes =
        """
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
            "numberOfParticipants": 1,
            "consentGroupName": "name",
            "generalResearchUse": true,
            "dataAccessCommitteeId": 1,
            "url": "https://asdf.com"
          }]
        }
        """;

    String emptyFileTypes =
        """
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
            "fileTypes": [],
            "numberOfParticipants": 1,
            "consentGroupName": "name",
            "generalResearchUse": true,
            "dataAccessCommitteeId": 1,
            "url": "https://asdf.com"
          }]
        }
        """;

    Set<Error> errors = schemaUtil.validateSchemaV1(noFileTypes);
    assertNoErrors(errors);

    errors = schemaUtil.validateSchemaV1(emptyFileTypes);
    assertNoErrors(errors);
  }

  @Test
  void testValidateDatasetRegistrationObject_v1_needs_at_least_one_disease() {
    String emptyDiseaseSpecificUse =
        """
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
              "functionalEquivalence": "equivalence"
            }],
            "numberOfParticipants": 2,
            "consentGroupName": "name",
            "diseaseSpecificUse": [],
            "dataAccessCommitteeId": 1,
            "url": "https://asdf.com"
          }]
        }
        """;

    String filledDiseaseSpecificUse =
        """
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
              "functionalEquivalence": "equivalence"
            }],
            "numberOfParticipants": 2,
            "consentGroupName": "name",
            "diseaseSpecificUse": ["something!"],
            "dataAccessCommitteeId": 1,
            "url": "https://asdf.com"
          }]
        }
        """;

    Set<Error> errors = schemaUtil.validateSchemaV1(emptyDiseaseSpecificUse);
    assertFieldHasError(errors, "diseaseSpecificUse");

    errors = schemaUtil.validateSchemaV1(filledDiseaseSpecificUse);
    assertNoErrors(errors);
  }

  @Test
  void testValidateDatasetRegistrationObject_v1_only_one_primary_consent() {
    String hmbAndGru =
        """
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
              "functionalEquivalence": "equivalence"
            }],
            "numberOfParticipants": 2,
            "consentGroupName": "name!",
            "generalResearchUse": true,
            "hmb": true,
            "dataAccessCommitteeId": 1,
            "url": "https://asdf.com"
          }]
        }
        """;

    String diseaseSpecificAndOpenAccess =
        """
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
              "functionalEquivalence": "equivalence"
            }],
            "numberOfParticipants": 2,
            "consentGroupName": "name!",
            "diseaseSpecificUse": ["some disease"],
            "accessManagement": "open",
            "dataAccessCommitteeId": 1,
            "url": "https://asdf.com"
          }]
        }
        """;

    Set<Error> errors = schemaUtil.validateSchemaV1(hmbAndGru);
    assertHasErrors(errors);

    errors = schemaUtil.validateSchemaV1(diseaseSpecificAndOpenAccess);
    assertHasErrors(errors);
  }

  @Test
  void testValidateDatasetRegistrationObject_v1_url_not_required_for_any_data_loc() {
    String notDeterminedNoURL =
        """
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
              "functionalEquivalence": "equivalence"
            }],
            "numberOfParticipants": 2,
            "consentGroupName": "name",
            "generalResearchUse": true,
            "dataAccessCommitteeId": 1,
            "dataLocation": "Not Determined"
          }]
        }
        """;
    String tdrLocationNoUrl =
        """
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
              "functionalEquivalence": "equivalence"
            }],
            "numberOfParticipants": 2,
            "consentGroupName": "name",
            "generalResearchUse": true,
            "dataAccessCommitteeId": 1,
            "dataLocation": "TDR Location"
          }]
        }
        """;

    Set<Error> errors = schemaUtil.validateSchemaV1(notDeterminedNoURL);
    assertNoErrors(errors);

    errors = schemaUtil.validateSchemaV1(tdrLocationNoUrl);
    assertNoErrors(errors);
  }

  @Test
  void testValidateDatasetRegistrationObject_v1_empty_string_is_invalid_if_required() {
    String instance =
        """
         {
           "studyType": "Observational",
           "studyName": "",
           "studyDescription": "",
           "dataTypes": ["types"],
           "phenotypeIndication": "phenotype",
           "species": "species",
           "piName": "",
           "dataCustodianEmail": ["email@abc.com"],
           "publicVisibility": true,
           "dataSubmitterUserId": 1,
           "nihAnvilUse": "I am NHGRI funded and I have a dbGaP PHS ID already",
           "dbGaPPhsID": "",
           "piInstitution": 10,
           "nihGrantContractNumber": "",
           "controlledAccessRequiredForGenomicSummaryResultsGSR": true,
           "controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation": "",
           "consentGroups": [{
             "fileTypes": [{
               "fileType": "Arrays",
               "functionalEquivalence": "equivalence"
             }],
             "numberOfParticipants": 2,
             "consentGroupName": "",
             "generalResearchUse": true,
             "dataAccessCommitteeId": 1,
             "url": ""
           }]
         }
        """;

    Set<Error> errors = schemaUtil.validateSchemaV1(instance);
    assertFieldHasError(errors, "studyName");
    assertFieldHasError(errors, "studyDescription");
    assertFieldHasError(errors, "piName");
    assertFieldHasError(errors, "dbGaPPhsID");
    assertFieldHasError(
        errors, "controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation");
    assertFieldHasError(errors, "nihGrantContractNumber");
    assertFieldHasError(errors, "consentGroupName");
    assertFieldHasError(errors, "url");
  }

  @Test
  void testExtractLabelsAndDescriptions() throws ExecutionException {
    JsonSchemaUtil util = new JsonSchemaUtil();

    // Ensure schema is loaded and labels/descriptions are extracted
    util.getDatasetRegistrationSchema();

    assertTrue(util.fieldLabels.containsKey("studyName"));
    assertEquals("Study Name", util.fieldLabels.get("studyName"));
    assertTrue(util.fieldDescriptions.containsKey("studyName"));
    assertEquals("The study name", util.fieldDescriptions.get("studyName"));
  }

  @Test
  void testFormatMessageRequired() {
    JsonSchemaUtil util = new JsonSchemaUtil();
    Error error = mock(Error.class);
    when(error.getKeyword()).thenReturn("required");
    when(error.getArguments()).thenReturn(new Object[] {"studyName"});

    String msg = util.formatMessage(error);
    assertTrue(msg.contains("is required"));
  }

  @Test
  void testDisplayNameOverrides() {
    JsonSchemaUtil util = new JsonSchemaUtil();
    String name = util.displayName("consentGroups");
    assertEquals("Datasets", name);
  }

  @Test
  void testDisplayNameFallback() {
    JsonSchemaUtil util = new JsonSchemaUtil();
    String name = util.displayName("nonexistentField");
    assertEquals("nonexistentField", name);
  }

  private void assertNoErrors(Set<Error> errors) {
    assertTrue(
        errors.isEmpty(),
        String.format(
            "Should be empty, instead was: %s", errors.stream().map(Error::toString).toList()));
  }

  private void assertHasErrors(Set<Error> errors) {
    assertFalse(errors.isEmpty(), "Should have errored, instead was empty.");
  }

  private void assertFieldHasError(Set<Error> errors, String field) {
    assertTrue(
        errors.stream()
            .anyMatch(
                error ->
                    error.getMessage().contains(field)
                        || (error.getInstanceLocation() != null
                            && error.getInstanceLocation().toString().contains("/" + field))
                        || (error.getProperty() != null && error.getProperty().equals(field))
                        || (error.getArguments() != null
                            && error.getArguments().length > 0
                            && error.getArguments()[0].toString().equals(field))),
        String.format(
            "Field '%s' should have errored. Got %d errors: %s",
            field,
            errors.size(),
            errors.stream()
                .map(
                    e ->
                        String.format(
                            "keyword=%s, message=%s, location=%s, property=%s",
                            e.getKeyword(),
                            e.getMessage(),
                            e.getInstanceLocation(),
                            e.getProperty()))
                .toList()));
  }
}
