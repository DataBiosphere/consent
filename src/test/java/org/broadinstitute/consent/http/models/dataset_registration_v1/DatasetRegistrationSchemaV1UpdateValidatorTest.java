package org.broadinstitute.consent.http.models.dataset_registration_v1;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatasetRegistrationSchemaV1UpdateValidatorTest {

  private DatasetRegistrationSchemaV1UpdateValidator validator;

  @BeforeEach
  void setUp() {
    validator = new DatasetRegistrationSchemaV1UpdateValidator();
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
    assertNull(registration.getConsentGroups().getFirst().getAccessManagement());
    assertNull(registration.getConsentGroups().getFirst().getCol());
    assertNull(registration.getConsentGroups().getFirst().getDataAccessCommitteeId());
    assertNull(registration.getConsentGroups().getFirst().getDatasetIdentifier());
    assertTrue(registration.getConsentGroups().getFirst().getDiseaseSpecificUse().isEmpty());
    assertNull(registration.getConsentGroups().getFirst().getGeneralResearchUse());
    assertNull(registration.getConsentGroups().getFirst().getGso());
    assertNull(registration.getConsentGroups().getFirst().getGs());
    assertNull(registration.getConsentGroups().getFirst().getHmb());
    assertNull(registration.getConsentGroups().getFirst().getIrb());
    assertNull(registration.getConsentGroups().getFirst().getMor());
    assertNull(registration.getConsentGroups().getFirst().getMorDate());
    assertNull(registration.getConsentGroups().getFirst().getNmds());
    assertNull(registration.getConsentGroups().getFirst().getNpu());
    assertNull(registration.getConsentGroups().getFirst().getOtherPrimary());
    assertNull(registration.getConsentGroups().getFirst().getOtherSecondary());
    assertNull(registration.getConsentGroups().getFirst().getPoa());
    assertNull(registration.getConsentGroups().getFirst().getPub());
    // Spot check some of the non-null expectations
    assertNotNull(registration.getStudyName());
    assertNotNull(registration.getPublicVisibility());
    assertNotNull(registration.getConsentGroups().getFirst().getDatasetId());
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
}
