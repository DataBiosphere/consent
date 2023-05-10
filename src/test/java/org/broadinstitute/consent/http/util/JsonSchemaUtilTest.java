package org.broadinstitute.consent.http.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.networknt.schema.ValidationMessage;
import java.util.Set;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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


    @BeforeAll
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
        Assertions.assertNotNull(instance);
        Assertions.assertNotNull(instance.getStudyType());
        Assertions.assertNotNull(instance.getStudyName());
        Assertions.assertNotNull(instance.getStudyDescription());
        assertFalse(instance.getDataTypes().isEmpty());
        Assertions.assertNotNull(instance.getPhenotypeIndication());
        Assertions.assertNotNull(instance.getSpecies());
        Assertions.assertNotNull(instance.getPiName());
        Assertions.assertNotNull(instance.getDataSubmitterUserId());
        assertFalse(instance.getDataCustodianEmail().isEmpty());
        Assertions.assertNotNull(instance.getPublicVisibility());
        assertFalse(instance.getConsentGroups().isEmpty());
        assertFalse(instance.getConsentGroups().get(0).getFileTypes().isEmpty());
        Assertions.assertNotNull(instance.getConsentGroups().get(0).getDataAccessCommitteeId());
    }

    @Test
    public void testValidateDatasetRegistrationObject_v1_valid_date() {
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
    public void testValidateDatasetRegistrationObject_v1_invalid_dates() {
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

    @Test
    public void testValidateDatasetRegistrationObject_v1_gsr_explanation_conditionally_required() {
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
        Set<ValidationMessage> errors = schemaUtil.validateSchema_v1(noGsrSelected);
        assertNoErrors(errors);

        errors = schemaUtil.validateSchema_v1(gsrSelectedNoExplanation);
        assertFieldHasError(errors, "controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation");

        errors = schemaUtil.validateSchema_v1(gsrSelectedWithExplanation);
        assertNoErrors(errors);
    }

    @Test
    public void testValidateDatasetRegistrationObject_v1_dbgap_info_conditionally_required() {
        String anvilUseNoWorks = datasetRegistrationInstance;

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

        Set<ValidationMessage> errors = schemaUtil.validateSchema_v1(anvilUseNoWorks);
        assertNoErrors(errors);

        errors = schemaUtil.validateSchema_v1(anvilUseYesRequiresDbGapFields);
        assertFieldHasError(errors, "dbGaPPhsID");
    }

    @Test
    public void testParseValidateRegistrationObject_v1_nih_admin_info_conditionally_required() {
        String anvilUseNoWorks = datasetRegistrationInstance;

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

        Set<ValidationMessage> errors = schemaUtil.validateSchema_v1(anvilUseNoWorks);
        assertNoErrors(errors);

        Set<ValidationMessage> fundedHaveIdErrors = schemaUtil.validateSchema_v1(anvilUseFundedHaveId);
        Set<ValidationMessage> fundedNoIdErrors = schemaUtil.validateSchema_v1(anvilUseFundedNoId);
        Set<ValidationMessage> seekingToSubmitErrors = schemaUtil.validateSchema_v1(anvilUseNotFundedSeekingToSubmit);

        assertFieldHasError(fundedHaveIdErrors, "piInstitution");
        assertFieldHasError(fundedNoIdErrors, "piInstitution");
        assertFieldHasError(seekingToSubmitErrors, "piInstitution");

        assertFieldHasError(fundedHaveIdErrors, "nihGrantContractNumber");
        assertFieldHasError(fundedNoIdErrors, "nihGrantContractNumber");
        assertFieldHasError(seekingToSubmitErrors, "nihGrantContractNumber");

    }

    @Test
    public void testParseValidateRegistrationObject_v1_dac_id_conditionally_required() {
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
                      "functionalEquivalence": "equivalence",
                      "numberOfParticipants": 2
                    }],
                    "consentGroupName": "name",
                    "openAccess": true,
                    "url": "https://asdf.com"
                  }]
                }
                """;

        String openAccessFalseNoDacId = """
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
                    "openAccess": false,
                    "poa": true,
                    "url": "https://asdf.com"
                  }]
                }
                """;

        String noOpenAccessNoDacId = """
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
                    "hmb": true,
                    "url": "https://asdf.com"
                  }]
                }
                """;

        String notOpenAccessWithDacId = datasetRegistrationInstance;

        Set<ValidationMessage> errors = schemaUtil.validateSchema_v1(openAccessNoDacId);
        assertNoErrors(errors);

        // only errors if not open access & no dac id present
        errors = schemaUtil.validateSchema_v1(openAccessFalseNoDacId);
        assertFieldHasError(errors, "dataAccessCommitteeId");
        errors = schemaUtil.validateSchema_v1(noOpenAccessNoDacId);
        assertFieldHasError(errors, "dataAccessCommitteeId");

        errors = schemaUtil.validateSchema_v1(notOpenAccessWithDacId);
        assertNoErrors(errors);
    }

    @Test
    public void testParseValidateRegistrationObject_v1_consent_group_required() {
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
    public void testValidateDatasetRegistrationObject_v1_file_types_required() {
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
                    "consentGroupName": "name",
                    "generalResearchUse": true,
                    "dataAccessCommitteeId": 1,
                    "url": "https://asdf.com"
                  }]
                }
                """;

        Set<ValidationMessage> errors = schemaUtil.validateSchema_v1(noFileTypes);
        assertFieldHasError(errors, "fileTypes");

        errors = schemaUtil.validateSchema_v1(emptyFileTypes);
        assertFieldHasError(errors, "fileTypes");
    }


    @Test
    public void testValidateDatasetRegistrationObject_v1_needs_at_least_one_disease() {
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
                      "functionalEquivalence": "equivalence",
                      "numberOfParticipants": 2
                    }],
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
                      "functionalEquivalence": "equivalence",
                      "numberOfParticipants": 2
                    }],                "consentGroupName": "name",
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
    public void testValidateDatasetRegistrationObject_v1_only_one_primary_consent() {
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
                      "functionalEquivalence": "equivalence",
                      "numberOfParticipants": 2
                    }],
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
                      "functionalEquivalence": "equivalence",
                      "numberOfParticipants": 2
                    }],
                    "consentGroupName": "name!",
                    "diseaseSpecificUse": ["some disease"],
                    "openAccess": true,
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
    public void testValidateDatasetRegistrationObject_v1_url_not_required_if_data_loc_not_determined() {
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
                      "functionalEquivalence": "equivalence",
                      "numberOfParticipants": 2
                    }],
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
                      "functionalEquivalence": "equivalence",
                      "numberOfParticipants": 2
                    }],
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
    public void testValidateDatasetRegistrationObject_v1_empty_string_is_invalid_if_required() {
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
                       "functionalEquivalence": "equivalence",
                       "numberOfParticipants": 2
                     }],
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
        assertFieldHasError(errors, "controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation");
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
