package org.broadinstitute.consent.http.models.draft;

import static org.broadinstitute.consent.http.models.draft.StudyBuilder.SPECIES;
import static org.broadinstitute.consent.http.models.draft.StudyBuilder.STUDY_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.broadinstitute.consent.http.models.DraftStudyDataset;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudyBuilderTest {

  // TODO Flesh out more test values
  String draftJson = """
      {
        "studyId": 6077,
        "studyName": "All of Us (Controlled+ Tier)",
        "studyType": "Descriptive",
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

  @Test
  void testBuildStudy() {
    User user = new User();
    user.setUserId(1);
    user.setEmail("email");
    StudyBuilder studyBuilder = new StudyBuilder();
    DraftStudyDataset draft = new DraftStudyDataset(draftJson, user);
    Study study = studyBuilder.buildStudyFromDraftLegacyRegistration(draft);
    assertNotNull(study);
    assertEquals(6077, study.getStudyId());
    assertEquals(draft.getName(), study.getName());
    assertEquals(draft.getCreateDate(), study.getCreateDate());
    assertEquals(draft.getUUID(), study.getUuid());
    assertEquals(user.getUserId(), study.getCreateUserId());
    assertEquals(user.getUserId(), study.getUpdateUserId());

    assertEquals("This study and dataset(s) represents the All of Us controlled+ tier data",
        study.getDescription());
    assertTrue(study.getDataTypes().contains("Whole Genome (WGS)"));
    assertEquals(true, study.getPublicVisibility());
    assertEquals("Paul Harris, Melissa Basford", study.getPiName());
    assertTrue(study.getProperties().stream()
        .anyMatch(p -> p.getKey().equals(SPECIES) && p.getValue().equals("Human")));
    assertTrue(study.getProperties().stream().anyMatch(
        p -> p.getKey().equals(STUDY_TYPE) && p.getValue().equals(StudyType.DESCRIPTIVE)));
    // TODO Flesh out testing
  }

}
