package org.broadinstitute.consent.http.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.networknt.schema.ValidationMessage;
import java.util.Set;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class JsonSchemaUtilTest {

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
    Set<ValidationMessage> errors = schemaUtil.validateSchema_v1(instance);
    assertFalse(errors.isEmpty());
  }

  @Test
  void testIsValidDatasetRegistrationObject_v1_case1() {
    Set<ValidationMessage> errors = schemaUtil.validateSchema_v1(datasetRegistrationInstance);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testParseDatasetRegistrationObject_v1() {
    DatasetRegistrationSchemaV1 instance = schemaUtil.deserializeDatasetRegistration(
        datasetRegistrationInstance);
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
  void testValidateDatasetRegistrationObject_v1_valid_date() {
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
    Set<ValidationMessage> errors = schemaUtil.validateSchema_v1(instance);
    assertNoErrors(errors);
  }

  @Test
  void testValidateDatasetRegistrationObject_v1_invalid_dates() {
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
    Set<ValidationMessage> errors = schemaUtil.validateSchema_v1(instance);
    assertFieldHasError(errors, "embargoReleaseDate");
    assertFieldHasError(errors, "targetDeliveryDate");
    assertFieldHasError(errors, "targetPublicReleaseDate");
  }

  @Test
  void testValidateDatasetRegistrationObject_v1_gsr_explanation_conditionally_required() {
    String noGsrSelected = """
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
    String gsrSelectedNoExplanation = """
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

    String gsrSelectedWithExplanation = """
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
    Set<ValidationMessage> errors = schemaUtil.validateSchema_v1(noGsrSelected);
    assertNoErrors(errors);

    errors = schemaUtil.validateSchema_v1(gsrSelectedNoExplanation);
    assertFieldHasError(errors,
        "controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation");

    errors = schemaUtil.validateSchema_v1(gsrSelectedWithExplanation);
    assertNoErrors(errors);
  }

  @Test
  void testValidateDatasetRegistrationObject_v1_dbgap_info_conditionally_required() {

    String anvilUseYesRequiresDbGapFields = """
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

    Set<ValidationMessage> errors = schemaUtil.validateSchema_v1(datasetRegistrationInstance);
    assertNoErrors(errors);

    errors = schemaUtil.validateSchema_v1(anvilUseYesRequiresDbGapFields);
    assertFieldHasError(errors, "dbGaPPhsID");
  }

  @Test
  void testParseValidateRegistrationObject_v1_nih_admin_info_conditionally_required() {

    String anvilUseFundedHaveId = """
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

    String anvilUseFundedNoId = """
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

    String anvilUseNotFundedSeekingToSubmit = """
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

    Set<ValidationMessage> errors = schemaUtil.validateSchema_v1(datasetRegistrationInstance);
    assertNoErrors(errors);

    Set<ValidationMessage> fundedHaveIdErrors = schemaUtil.validateSchema_v1(anvilUseFundedHaveId);
    Set<ValidationMessage> fundedNoIdErrors = schemaUtil.validateSchema_v1(anvilUseFundedNoId);
    Set<ValidationMessage> seekingToSubmitErrors = schemaUtil.validateSchema_v1(
        anvilUseNotFundedSeekingToSubmit);

    assertFieldHasError(fundedHaveIdErrors, "piInstitution");
    assertFieldHasError(fundedNoIdErrors, "piInstitution");
    assertFieldHasError(seekingToSubmitErrors, "piInstitution");

    assertFieldHasError(fundedHaveIdErrors, "nihGrantContractNumber");
    assertFieldHasError(fundedNoIdErrors, "nihGrantContractNumber");
    assertFieldHasError(seekingToSubmitErrors, "nihGrantContractNumber");

  }

  @Test
  void testParseValidateRegistrationObject_v1_dac_id_conditionally_required() {
    String openAccessNoDacId = """
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

    String controlledAccessNoDacId = """
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

    String noAccessManagementNoDacId = """
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

    Set<ValidationMessage> errors = schemaUtil.validateSchema_v1(openAccessNoDacId);
    assertNoErrors(errors);

    // only errors if not open access & no dac id present
    errors = schemaUtil.validateSchema_v1(controlledAccessNoDacId);
    assertFieldHasError(errors, "dataAccessCommitteeId");
    errors = schemaUtil.validateSchema_v1(noAccessManagementNoDacId);
    assertFieldHasError(errors, "dataAccessCommitteeId");

    errors = schemaUtil.validateSchema_v1(datasetRegistrationInstance);
    assertNoErrors(errors);
  }

  @Test
  void testParseValidateRegistrationObject_v1_consent_group_required() {
    String noConsentGroup = """
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

    String emptyConsentGroup = """
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

    Set<ValidationMessage> errors = schemaUtil.validateSchema_v1(noConsentGroup);
    assertFieldHasError(errors, "consentGroups");

    errors = schemaUtil.validateSchema_v1(emptyConsentGroup);
    assertFieldHasError(errors, "consentGroups");
  }

  @Test
  void testValidateDatasetRegistrationObject_v1_file_types_not_required() {
    String noFileTypes = """
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

    String emptyFileTypes = """
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

    Set<ValidationMessage> errors = schemaUtil.validateSchema_v1(noFileTypes);
    assertNoErrors(errors);

    errors = schemaUtil.validateSchema_v1(emptyFileTypes);
    assertNoErrors(errors);
  }


  @Test
  void testValidateDatasetRegistrationObject_v1_needs_at_least_one_disease() {
    String emptyDiseaseSpecificUse = """
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

    String filledDiseaseSpecificUse = """
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

    Set<ValidationMessage> errors = schemaUtil.validateSchema_v1(emptyDiseaseSpecificUse);
    assertFieldHasError(errors, "diseaseSpecificUse");

    errors = schemaUtil.validateSchema_v1(filledDiseaseSpecificUse);
    assertNoErrors(errors);
  }

  @Test
  void testValidateDatasetRegistrationObject_v1_only_one_primary_consent() {
    String hmbAndGru = """
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

    String diseaseSpecificAndOpenAccess = """
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

    Set<ValidationMessage> errors = schemaUtil.validateSchema_v1(hmbAndGru);
    assertHasErrors(errors);

    errors = schemaUtil.validateSchema_v1(diseaseSpecificAndOpenAccess);
    assertHasErrors(errors);
  }

  @Test
  void testValidateDatasetRegistrationObject_v1_url_not_required_if_data_loc_not_determined() {
    String notDeterminedNoURL = """
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
    ;
    String tdrLocationNoUrl = """
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

    Set<ValidationMessage> errors = schemaUtil.validateSchema_v1(notDeterminedNoURL);
    assertNoErrors(errors);

    errors = schemaUtil.validateSchema_v1(tdrLocationNoUrl);
    assertFieldHasError(errors, "url");

  }


  @Test
  void testValidateDatasetRegistrationObject_v1_empty_string_is_invalid_if_required() {
    String instance = """
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

    Set<ValidationMessage> errors = schemaUtil.validateSchema_v1(instance);
    assertFieldHasError(errors, "studyName");
    assertFieldHasError(errors, "studyDescription");
    assertFieldHasError(errors, "piName");
    assertFieldHasError(errors, "dbGaPPhsID");
    assertFieldHasError(errors,
        "controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation");
    assertFieldHasError(errors, "nihGrantContractNumber");
    assertFieldHasError(errors, "consentGroupName");
    assertFieldHasError(errors, "url");
  }


  private void assertNoErrors(Set<ValidationMessage> errors) {
    assertTrue(errors.isEmpty(),
        String.format("Should be empty, instead was: %s", errors.stream().map(
            ValidationMessage::toString).toList()));
  }

  private void assertHasErrors(Set<ValidationMessage> errors) {
    assertFalse(errors.isEmpty(), "Should have errored, instead was empty.");
  }

  private void assertFieldHasError(Set<ValidationMessage> errors, String field) {
    assertTrue(
        errors.stream().anyMatch((ValidationMessage s) -> s.getMessage().contains(field)),
        String.format("Field %s should have errored", field));
  }
}
