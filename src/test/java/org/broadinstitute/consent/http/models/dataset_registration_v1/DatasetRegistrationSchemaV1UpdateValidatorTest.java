package org.broadinstitute.consent.http.models.dataset_registration_v1;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.BadRequestException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.DataLocation;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.NihAnvilUse;
import org.broadinstitute.consent.http.service.DatasetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetRegistrationSchemaV1UpdateValidatorTest {

  private static final Study STATIC_STUDY;
  private static final Integer STATIC_DATASET_ID;

  static {
    STATIC_STUDY = new Study();
    STATIC_STUDY.setName("TestStudy");
    Dataset dataset = new Dataset();
    dataset.setName("");
    dataset.setDatasetId(16); // Use a fixed ID for consistency
    dataset.setDacId(42);
    STATIC_STUDY.addDatasets(List.of(dataset));
    STATIC_STUDY.addDatasetIds(Set.of(dataset.getDatasetId()));
    STATIC_DATASET_ID = dataset.getDatasetId();
  }

  @Mock private DatasetService datasetService;
  private DatasetRegistrationSchemaV1UpdateValidator validator;

  @BeforeEach
  void setUp() {
    validator = new DatasetRegistrationSchemaV1UpdateValidator(datasetService);
  }

  @Test
  void testValidation_knownFieldsExcluded() {
    String json =
        """
        {
          "studyId": 6077,
          "studyName": "All of Us (Controlled+ Tier)",
          "studyType": null,
          "studyDescription": "This study and dataset(s) represents the All of Us controlled+ tier data",
          "dataTypes": [
            "Whole Genome (WGS)"
          ],
          "phenotypeIndication": "NA",
          "species": "Human",
          "piName": "Paul Harris, Melissa Basford",
          "dataSubmitterUserId": 3396,
          "dataCustodianEmail": ["email@test.com"],
          "publicVisibility": true,
          "nihAnvilUse": "I_AM_NOT_NHGRI_FUNDED_AND_DO_NOT_PLAN_TO_STORE_DATA_IN_AN_VIL",
          "submittingToAnvil": null,
          "dbGaPPhsID": null,
          "dbGaPStudyRegistrationName": null,
          "embargoReleaseDate": null,
          "sequencingCenter": null,
          "piInstitution": null,
          "nihGrantContractNumber": null,
          "nihICsSupportingStudy": [],
          "nihProgramOfficerName": null,
          "nihInstitutionCenterSubmission": null,
          "nihGenomicProgramAdministratorName": null,
          "multiCenterStudy": null,
          "collaboratingSites": [],
          "controlledAccessRequiredForGenomicSummaryResultsGSR": null,
          "controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation": null,
          "alternativeDataSharingPlan": null,
          "alternativeDataSharingPlanReasons": [],
          "alternativeDataSharingPlanExplanation": null,
          "alternativeDataSharingPlanFileName": null,
          "alternativeDataSharingPlanDataSubmitted": null,
          "alternativeDataSharingPlanDataReleased": null,
          "alternativeDataSharingPlanTargetDeliveryDate": null,
          "alternativeDataSharingPlanTargetPublicReleaseDate": null,
          "alternativeDataSharingPlanAccessManagement": null,
          "consentGroups": [
            {
              "datasetId": 2583,
              "datasetIdentifier": "DUOS-001078",
              "consentGroupName": "All of Us (Controlled+ Tier) - Set 1",
              "accessManagement": "OPEN",
              "generalResearchUse": true,
              "hmb": false,
              "diseaseSpecificUse": ["https://disease-ontology.org/term/DOID:162/"],
              "poa": true,
              "otherPrimary": "String",
              "nmds": true,
              "gso": false,
              "pub": false,
              "col": false,
              "irb": false,
              "gs": "GS",
              "mor": true,
              "morDate": "More Date",
              "npu": false,
              "otherSecondary": "String",
              "dataAccessCommitteeId": 99,
              "dataLocation": "TDR_LOCATION",
              "url": "https://abcnews.com",
              "numberOfParticipants": 2,
              "fileTypes": [
                {
                  "fileType": null,
                  "functionalEquivalence": null
                }
              ]
            },
            {
              "consentGroupName": "All of Us (Controlled+ Tier) - Set 2",
              "accessManagement": "OPEN",
              "generalResearchUse": true,
              "hmb": false,
              "diseaseSpecificUse": ["https://disease-ontology.org/term/DOID:162/"],
              "poa": true,
              "otherPrimary": "String",
              "nmds": true,
              "gso": false,
              "pub": false,
              "col": false,
              "irb": false,
              "gs": "GS",
              "mor": true,
              "morDate": "More Date",
              "npu": false,
              "otherSecondary": "String",
              "dataAccessCommitteeId": 99,
              "dataLocation": "TDR_LOCATION",
              "url": "https://abcnews.com",
              "numberOfParticipants": 2,
              "fileTypes": [
                {
                  "fileType": null,
                  "functionalEquivalence": null
                }
              ]
            }
          ]
        }
        """;
    DatasetRegistrationSchemaV1 registration = validator.deserializeRegistration(json);
    assertNull(registration.getDataSubmitterUserId());
    assertNull(registration.getConsentGroups().get(0).getAccessManagement());
    assertNull(registration.getConsentGroups().get(0).getCol());
    assertNull(registration.getConsentGroups().get(0).getDataAccessCommitteeId());
    assertNull(registration.getConsentGroups().get(0).getDatasetIdentifier());
    assertTrue(registration.getConsentGroups().get(0).getDiseaseSpecificUse().isEmpty());
    assertNull(registration.getConsentGroups().get(0).getGeneralResearchUse());
    assertNull(registration.getConsentGroups().get(0).getGso());
    assertNull(registration.getConsentGroups().get(0).getGs());
    assertNull(registration.getConsentGroups().get(0).getHmb());
    assertNull(registration.getConsentGroups().get(0).getIrb());
    assertNull(registration.getConsentGroups().get(0).getMor());
    assertNull(registration.getConsentGroups().get(0).getMorDate());
    assertNull(registration.getConsentGroups().get(0).getNmds());
    assertNull(registration.getConsentGroups().get(0).getNpu());
    assertNull(registration.getConsentGroups().get(0).getOtherPrimary());
    assertNull(registration.getConsentGroups().get(0).getOtherSecondary());
    assertNull(registration.getConsentGroups().get(0).getPoa());
    assertNull(registration.getConsentGroups().get(0).getPub());
    // Spot check some of the non-null expectations
    assertNotNull(registration.getStudyName());
    assertNotNull(registration.getPublicVisibility());
    assertNotNull(registration.getConsentGroups().get(0).getDatasetId());
    // Assert that the second consent group is populated with data use values
    assertNotNull(registration.getConsentGroups().get(1).getAccessManagement());
    assertNotNull(registration.getConsentGroups().get(1).getCol());
    assertNotNull(registration.getConsentGroups().get(1).getDataAccessCommitteeId());
    assertFalse(registration.getConsentGroups().get(1).getDiseaseSpecificUse().isEmpty());
    assertNotNull(registration.getConsentGroups().get(1).getGeneralResearchUse());
    assertNotNull(registration.getConsentGroups().get(1).getGso());
    assertNotNull(registration.getConsentGroups().get(1).getGs());
    assertNotNull(registration.getConsentGroups().get(1).getHmb());
    assertNotNull(registration.getConsentGroups().get(1).getIrb());
    assertNotNull(registration.getConsentGroups().get(1).getMor());
    assertNotNull(registration.getConsentGroups().get(1).getMorDate());
    assertNotNull(registration.getConsentGroups().get(1).getNmds());
    assertNotNull(registration.getConsentGroups().get(1).getNpu());
    assertNotNull(registration.getConsentGroups().get(1).getOtherPrimary());
    assertNotNull(registration.getConsentGroups().get(1).getOtherSecondary());
    assertNotNull(registration.getConsentGroups().get(1).getPoa());
    assertNotNull(registration.getConsentGroups().get(1).getPub());
    assertNotNull(registration.getConsentGroups().get(1).getDataLocation());
    assertNotNull(registration.getConsentGroups().get(1).getUrl());
    assertFalse(registration.getConsentGroups().get(1).getFileTypes().isEmpty());
  }

  @Test
  void testValidation_valid() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);

    boolean valid = validator.validate(study, registration);
    assertTrue(valid);
  }

  @Test
  void testValidation_valid_study_name_change() {
    Study study = createMockStudy();
    when(datasetService.findAllStudyNames()).thenReturn(Set.of(study.getName()));
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setStudyName("New Name");

    boolean valid = validator.validate(study, registration);
    assertTrue(valid);
  }

  @Test
  void testValidation_invalid_study_name_change() {
    String existingStudyName = RandomStringUtils.randomAlphabetic(10);
    Study study = createMockStudy();
    when(datasetService.findAllStudyNames()).thenReturn(Set.of(study.getName(), existingStudyName));
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setStudyName(existingStudyName);

    assertThrows(
        BadRequestException.class,
        () -> {
          validator.validate(study, registration);
        });
  }

  @Test
  void testValidation_empty_consent_groups() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setConsentGroups(List.of());

    assertThrows(
        BadRequestException.class,
        () -> {
          validator.validate(study, registration);
        });
  }

  @Test
  void testValidation_non_study_dataset() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    // mock data is limited to 10->100
    registration.getConsentGroups().get(0).setDatasetId(10000);

    assertThrows(
        BadRequestException.class,
        () -> {
          validator.validate(study, registration);
        });
  }

  @Test
  void testValidation_consent_group_name_change_allowed() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    study
        .getDatasets()
        .forEach(
            d -> {
              d.setName("");
            });

    boolean valid = validator.validate(study, registration);
    assertTrue(valid);
  }

  @Test
  void testValidation_consent_group_name_change_not_allowed() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    study
        .getDatasets()
        .forEach(
            d -> {
              d.setName("Existing Name");
            });

    assertThrows(
        BadRequestException.class,
        () -> {
          validator.validate(study, registration);
        });
  }

  @Test
  void testValidation_consent_group_data_location_required() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration
        .getConsentGroups()
        .forEach(
            cg -> {
              cg.setDataLocation(null);
            });

    assertThrows(
        BadRequestException.class,
        () -> {
          validator.validate(study, registration);
        });
  }

  @Test
  void testValidation_invalid_delete_consent_groups() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    Dataset dataset = new Dataset();
    dataset.setName(RandomStringUtils.randomAlphabetic(10));
    // mock data is limited to 10->100
    dataset.setDatasetId(10000);
    dataset.setDacId(RandomUtils.nextInt(10, 100));
    study.getDatasets().add(dataset);
    ArrayList<Integer> datasetIds = new ArrayList<>(study.getDatasetIds().stream().toList());
    datasetIds.add(dataset.getDatasetId());
    study.addDatasetIds(new HashSet<>(datasetIds));

    assertThrows(
        BadRequestException.class,
        () -> {
          validator.validate(study, registration);
        });
  }

  @Test
  void testValidation_study_description() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setStudyDescription(null);

    assertThrows(
        BadRequestException.class,
        () -> {
          validator.validate(study, registration);
        });
  }

  @Test
  void testValidation_data_types() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setDataTypes(List.of());

    assertThrows(
        BadRequestException.class,
        () -> {
          validator.validate(study, registration);
        });
  }

  @Test
  void testValidation_public_visibility() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setPublicVisibility(null);

    assertThrows(
        BadRequestException.class,
        () -> {
          validator.validate(study, registration);
        });
  }

  @Test
  void testValidation_nih_anvil_use() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setNihAnvilUse(null);

    assertThrows(
        BadRequestException.class,
        () -> {
          validator.validate(study, registration);
        });
  }

  @Test
  void testValidation_dbgap_phsid() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setNihAnvilUse(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY);
    registration.setDbGaPPhsID(null);

    assertThrows(
        BadRequestException.class,
        () -> {
          validator.validate(study, registration);
        });
  }

  @Test
  void testValidation_dbgap_pi_institution() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setNihAnvilUse(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY);
    registration.setDbGaPPhsID(RandomStringUtils.randomAlphabetic(10));
    registration.setPiInstitution(null);

    assertThrows(
        BadRequestException.class,
        () -> {
          validator.validate(study, registration);
        });
  }

  @Test
  void testValidation_nih_grant_contract_number() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setNihAnvilUse(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY);
    registration.setDbGaPPhsID(RandomStringUtils.randomAlphabetic(10));
    registration.setPiInstitution(RandomUtils.nextInt(10, 100));
    registration.setNihGrantContractNumber(null);

    assertThrows(
        BadRequestException.class,
        () -> {
          validator.validate(study, registration);
        });
  }

  @Test
  void testValidation_pi_institution() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setNihAnvilUse(
        NihAnvilUse.I_AM_NOT_NHGRI_FUNDED_BUT_I_AM_SEEKING_TO_SUBMIT_DATA_TO_AN_VIL);
    registration.setPiInstitution(null);

    assertThrows(
        BadRequestException.class,
        () -> {
          validator.validate(study, registration);
        });
  }

  @Test
  void testValidation_pi_institution_nih_grant_contract_number() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setNihAnvilUse(
        NihAnvilUse.I_AM_NOT_NHGRI_FUNDED_BUT_I_AM_SEEKING_TO_SUBMIT_DATA_TO_AN_VIL);
    registration.setPiInstitution(RandomUtils.nextInt(10, 100));
    registration.setNihGrantContractNumber(null);

    assertThrows(
        BadRequestException.class,
        () -> {
          validator.validate(study, registration);
        });
  }

  @Test
  void testValidation_pi_name() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setPiName(null);

    assertThrows(
        BadRequestException.class,
        () -> {
          validator.validate(study, registration);
        });
  }

  @ParameterizedTest
  @MethodSource("assetRequiredFieldProvider")
  void testAssetRequiredFields(String json, String expectedMessage) {
    DatasetRegistrationSchemaV1 registration = validator.deserializeRegistration(json);
    BadRequestException ex =
        assertThrows(
            BadRequestException.class, () -> validator.validate(STATIC_STUDY, registration));
    System.out.println(ex.getMessage());
    assertTrue(ex.getMessage().contains(expectedMessage));
  }

  static Stream<Arguments> assetRequiredFieldProvider() {
    String baseJson =
        """
      {
        "studyName": "Test Study",
        "studyDescription": "desc",
        "dataTypes": ["Genomic"],
        "publicVisibility": true,
        "nihAnvilUse": "I_AM_NOT_NHGRI_FUNDED_AND_DO_NOT_PLAN_TO_STORE_DATA_IN_AN_VIL",
        "piName": "PI",
        "consentGroups": [
          {
            "consentGroupName": "Group 1",
            "numberOfParticipants": 100,
            "accessManagement": "open",
            "dataLocation": "TDR_LOCATION",
            "datasetId": %d
          }
        ],
        "assets": %s
      }
      """;

    return Stream.of(
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"models":[{"name":"","url":"https://example.com","format":"format","license":"license","maintainer":{"name":"Name","email":"email@example.com"}}]}
        """),
            "AI Model name is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"models":[{"name":"Model","url":null,"format":"format","license":"license","maintainer":{"name":"Name","email":"email@example.com"}}]}
        """),
            "AI Model url is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"models":[{"name":"Model","url":"invalid-url","format":"format","license":"license","maintainer":{"name":"Name","email":"email@example.com"}}]}
        """),
            "AI Model url is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"models":[{"name":"Model","url":"https://example.com","format":"","license":"license","maintainer":{"name":"Name","email":"email@example.com"}}]}
        """),
            "AI Model format is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"models":[{"name":"Model","url":"https://example.com","format":"format","license":"","maintainer":{"name":"Name","email":"email@example.com"}}]}
        """),
            "AI Model license is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"models":[{"name":"Model","url":"https://example.com","format":"format","license":"license","maintainer":null}]}
        """),
            "AI Model maintainer name and email are required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"models":[{"name":"Model","url":"https://example.com","format":"format","license":"license","maintainer":{"name":"","email":"email@example.com"}}]}
        """),
            "AI Model maintainer name and email are required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"models":[{"name":"Model","url":"https://example.com","format":"format","license":"license","maintainer":{"name":"Name","email":""}}]}
        """),
            "AI Model maintainer name and email are required"),

        // Workspace required fields
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"workspaces":[{"name":"","platform":"Terra","url":"https://workspace.com","description":"desc"}]}
        """),
            "Workspace name is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"workspaces":[{"name":"WS","platform":"","url":"https://workspace.com","description":"desc"}]}
        """),
            "Workspace platform is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"workspaces":[{"name":"WS","platform":"Terra","url":null,"description":"desc"}]}
        """),
            "Workspace url is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"workspaces":[{"name":"WS","platform":"Terra","url":"invalid-url","description":"desc"}]}
        """),
            "Workspace url is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"workspaces":[{"name":"WS","platform":"Terra","url":"https://workspace.com","description":""}]}
        """),
            "Workspace description is required"),

        // Presentation required fields
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"presentations":[{"title":"","date":"2024-01-01"}]}
        """),
            "Presentation title is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"presentations":[{"title":"Pres","date":null}]}
        """),
            "Presentation date is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"presentations":[{"title":"Pres","date":"invalid-date"}]}
        """),
            "Presentation date is required"),

        // Publication required fields
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"publications":[{"title":"","publishedDate":"2024-01-01","datasetCitation":"cite","journal":"journal","doi":"doi"}]}
        """),
            "Publication title is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"publications":[{"title":"Pub","publishedDate":null,"datasetCitation":"cite","journal":"journal","doi":"doi"}]}
        """),
            "Publication publishedDate is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"publications":[{"title":"Pub","publishedDate":"invalid-date","datasetCitation":"cite","journal":"journal","doi":"doi"}]}
        """),
            "Publication publishedDate is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"publications":[{"title":"Pub","publishedDate":"2024-01-01","datasetCitation":"","journal":"journal","doi":"doi"}]}
        """),
            "Publication datasetCitation is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"publications":[{"title":"Pub","publishedDate":"2024-01-01","datasetCitation":"cite","journal":"","doi":"doi"}]}
        """),
            "Publication journal is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"publications":[{"title":"Pub","publishedDate":"2024-01-01","datasetCitation":"cite","journal":"journal","doi":""}]}
        """),
            "Publication doi is required"),

        // ClinicalTrial required fields
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"clinicalTrials":[{"title":"","registry":"reg","identifier":"id","status":"COMPLETED","sponsor":"sponsor","startDate":"2024-01-01","interventionType":"DRUG","phase":"PHASE2","url":"https://trial.com"}]}
        """),
            "Clinical Trial title is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"clinicalTrials":[{"title":"CT","registry":"","identifier":"id","status":"COMPLETED","sponsor":"sponsor","startDate":"2024-01-01","interventionType":"DRUG","phase":"PHASE2","url":"https://trial.com"}]}
        """),
            "Clinical Trial registry is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"clinicalTrials":[{"title":"CT","registry":"reg","identifier":"","status":"COMPLETED","sponsor":"sponsor","startDate":"2024-01-01","interventionType":"DRUG","phase":"PHASE2","url":"https://trial.com"}]}
        """),
            "Clinical Trial identifier is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"clinicalTrials":[{"title":"CT","registry":"reg","identifier":"id","status":null,"sponsor":"sponsor","startDate":"2024-01-01","interventionType":"DRUG","phase":"PHASE2","url":"https://trial.com"}]}
        """),
            "Clinical Trial status is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"clinicalTrials":[{"title":"CT","registry":"reg","identifier":"id","status":"COMPLETED","sponsor":"","startDate":"2024-01-01","interventionType":"DRUG","phase":"PHASE2","url":"https://trial.com"}]}
        """),
            "Clinical Trial sponsor is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"clinicalTrials":[{"title":"CT","registry":"reg","identifier":"id","status":"COMPLETED","sponsor":"sponsor","startDate":null,"interventionType":"DRUG","phase":"PHASE2","url":"https://trial.com"}]}
        """),
            "Clinical Trial startDate is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"clinicalTrials":[{"title":"CT","registry":"reg","identifier":"id","status":"COMPLETED","sponsor":"sponsor","startDate":"invalid-date","interventionType":"DRUG","phase":"PHASE2","url":"https://trial.com"}]}
        """),
            "Clinical Trial startDate is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"clinicalTrials":[{"title":"CT","registry":"reg","identifier":"id","status":"COMPLETED","sponsor":"sponsor","startDate":"2024-01-01","interventionType":null,"phase":"PHASE2","url":"https://trial.com"}]}
        """),
            "Clinical Trial interventionType is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"clinicalTrials":[{"title":"CT","registry":"reg","identifier":"id","status":"COMPLETED","sponsor":"sponsor","startDate":"2024-01-01","interventionType":"DRUG","phase":null,"url":"https://trial.com"}]}
        """),
            "Clinical Trial phase is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"clinicalTrials":[{"title":"CT","registry":"reg","identifier":"id","status":"COMPLETED","sponsor":"sponsor","startDate":"2024-01-01","interventionType":"DRUG","phase":"PHASE2","url":null}]}
        """),
            "Clinical Trial url is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"clinicalTrials":[{"title":"CT","registry":"reg","identifier":"id","status":"COMPLETED","sponsor":"sponsor","startDate":"2024-01-01","interventionType":"DRUG","phase":"PHASE2","url":"invalid-url"}]}
        """),
            "Clinical Trial url is required"),

        // FundingResource required fields
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"funding":[{"funderName":"","funderProgram":"prog","grantNumber":"grant","projectTitle":"title"}]}
        """),
            "FundingResource funderName is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"funding":[{"funderName":"Funder","funderProgram":"","grantNumber":"grant","projectTitle":"title"}]}
        """),
            "FundingResource funderProgram is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"funding":[{"funderName":"Funder","funderProgram":"prog","grantNumber":"","projectTitle":"title"}]}
        """),
            "FundingResource grantNumber is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"funding":[{"funderName":"Funder","funderProgram":"prog","grantNumber":"grant","projectTitle":""}]}
        """),
            "FundingResource projectTitle is required"),

        // IntellectualProperty required fields
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"intellectualProperties":[{"type":"","title":"title","assignee":"assignee","patentNumber":"patent","filingDate":"2024-01-01","status":"status","url":"https://ip.com","contact":"contact"}]}
        """),
            "Intellectual Property type is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"intellectualProperties":[{"type":"type","title":"","assignee":"assignee","patentNumber":"patent","filingDate":"2024-01-01","status":"status","url":"https://ip.com","contact":"contact"}]}
        """),
            "Intellectual Property title is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"intellectualProperties":[{"type":"type","title":"title","assignee":"","patentNumber":"patent","filingDate":"2024-01-01","status":"status","url":"https://ip.com","contact":"contact"}]}
        """),
            "Intellectual Property assignee is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"intellectualProperties":[{"type":"type","title":"title","assignee":"assignee","patentNumber":"","filingDate":"2024-01-01","status":"status","url":"https://ip.com","contact":"contact"}]}
        """),
            "Intellectual Property patentNumber is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"intellectualProperties":[{"type":"type","title":"title","assignee":"assignee","patentNumber":"patent","filingDate":null,"status":"status","url":"https://ip.com","contact":"contact"}]}
        """),
            "Intellectual Property filingDate is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"intellectualProperties":[{"type":"type","title":"title","assignee":"assignee","patentNumber":"patent","filingDate":"invalid-date","status":"status","url":"https://ip.com","contact":"contact"}]}
        """),
            "Intellectual Property filingDate is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"intellectualProperties":[{"type":"type","title":"title","assignee":"assignee","patentNumber":"patent","filingDate":"2024-01-01","status":"","url":"https://ip.com","contact":"contact"}]}
        """),
            "Intellectual Property status is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"intellectualProperties":[{"type":"type","title":"title","assignee":"assignee","patentNumber":"patent","filingDate":"2024-01-01","status":"status","url":null,"contact":"contact"}]}
        """),
            "Intellectual Property url is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"intellectualProperties":[{"type":"type","title":"title","assignee":"assignee","patentNumber":"patent","filingDate":"2024-01-01","status":"status","url":"invalid-url","contact":"contact"}]}
        """),
            "Intellectual Property url is required"),
        Arguments.of(
            String.format(
                baseJson,
                STATIC_DATASET_ID,
                """
          {"intellectualProperties":[{"type":"type","title":"title","assignee":"assignee","patentNumber":"patent","filingDate":"2024-01-01","status":"status","url":"https://ip.com","contact":""}]}
        """),
            "Intellectual Property contact is required"));
  }

  private Study createMockStudy() {
    Study study = new Study();
    study.setName(RandomStringUtils.randomAlphabetic(10));
    Dataset dataset = new Dataset();
    dataset.setName("");
    dataset.setDatasetId(RandomUtils.nextInt(10, 100));
    dataset.setDacId(RandomUtils.nextInt(10, 100));
    study.addDatasets(List.of(dataset));
    study.addDatasetIds(Set.of(dataset.getDatasetId()));
    return study;
  }

  private DatasetRegistrationSchemaV1 createMockRegistration(Study study) {
    DatasetRegistrationSchemaV1 registration = new DatasetRegistrationSchemaV1();
    registration.setStudyName(study.getName());
    registration.setStudyDescription(RandomStringUtils.randomAlphabetic(10));
    registration.setDataTypes(List.of(RandomStringUtils.randomAlphabetic(10)));
    registration.setPublicVisibility(true);
    registration.setNihAnvilUse(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY);
    registration.setDbGaPPhsID(RandomStringUtils.randomAlphabetic(10));
    registration.setPiInstitution(RandomUtils.nextInt(1, 100));
    registration.setNihGrantContractNumber(RandomStringUtils.randomAlphabetic(10));
    registration.setPhenotypeIndication(RandomStringUtils.randomAlphabetic(10));
    registration.setPiName(RandomStringUtils.randomAlphabetic(10));
    registration.setDataCustodianEmail(List.of(RandomStringUtils.randomAlphabetic(10)));
    List<ConsentGroup> cgs =
        study.getDatasets().stream()
            .map(
                d -> {
                  ConsentGroup cg = new ConsentGroup();
                  cg.setDataLocation(DataLocation.NOT_DETERMINED);
                  cg.setNumberOfParticipants(RandomUtils.nextInt(10, 100));
                  cg.setConsentGroupName(RandomStringUtils.randomAlphabetic(10));
                  cg.setDatasetId(d.getDatasetId());
                  cg.setDataAccessCommitteeId(d.getDacId());
                  return cg;
                })
            .toList();
    registration.setConsentGroups(cgs);
    return registration;
  }
}
