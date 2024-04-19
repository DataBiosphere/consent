package org.broadinstitute.consent.http.resources;

public class DataResourceTestData {

  public static final String registrationWithMalformedJson = """
      {
        "studyType" - "Observational"
      }
      """;

  public static final String registrationWithStudyName = """
      {
        "studyType": "Observational",
        "studyName": "name",
        "studyDescription": "description",
        "dataTypes": ["types"],
        "phenotypeIndication": "Phenotype",
        "species": "species",
        "piName": "PI Name"
      }
      """;

  public static final String registrationWithDataSubmitterUserId = """
      {
        "studyType": "Observational",
        "studyDescription": "description",
        "dataTypes": ["types"],
        "phenotypeIndication": "Phenotype",
        "species": "species",
        "piName": "PI Name",
        "dataSubmitterUserId": 1000000
      }
      """;

  public static final String registrationWithExistingCGDataUse = """
      {
        "studyType": "Observational",
        "studyDescription": "description",
        "dataTypes": ["types"],
        "phenotypeIndication": "Phenotype",
        "species": "species",
        "piName": "PI Name",
        "consentGroups": [
          {
            "datasetId": 1,
            "fileTypes": [{
              "fileType": "Arrays",
              "functionalEquivalence": "equivalence"
            }],
            "numberOfParticipants": 2,
            "consentGroupName": "name 1",
            "generalResearchUse": true,
            "dataAccessCommitteeId": 1,
            "url": "https://asdf.com"
          },
          {
            "datasetId": 2,
            "fileTypes": [{
              "fileType": "Arrays",
              "functionalEquivalence": "equivalence"
            }],
            "numberOfParticipants": 2,
            "consentGroupName": "name 2",
            "hmb": true,
            "dataAccessCommitteeId": 1,
            "url": "https://asdf.com"
          }
        ]
      }
      """;

  public static final String registrationWithExistingCG = """
      {
        "studyType": "Observational",
        "studyDescription": "description",
        "dataTypes": ["types"],
        "phenotypeIndication": "Phenotype",
        "species": "species",
        "piName": "PI Name",
        "consentGroups": [
          {
            "datasetId": 1,
            "fileTypes": [{
              "fileType": "Arrays",
              "functionalEquivalence": "equivalence"
            }],
            "numberOfParticipants": 2,
            "consentGroupName": "name 1",
            "dataAccessCommitteeId": 1,
            "url": "https://asdf.com"
          }
        ]
      }
      """;

  public static final String validRegistration = """
      {
        "studyType": "Observational",
        "studyDescription": "description",
        "dataTypes": ["types"],
        "phenotypeIndication": "Phenotype",
        "species": "species",
        "piName": "PI Name",
        "dataCustodianEmail": ["Custodian Email"],
        "publicVisibility": true,
        "nihAnvilUse": "I am NHGRI funded and I have a dbGaP PHS ID already",
        "submittingToAnvil": false,
        "dbGaPPhsID": "ID",
        "dbGaPStudyRegistrationName": "Name",
        "embargoReleaseDate": "embargo date",
        "sequencingCenter": "center",
        "piInstitution": 150,
        "nihGrantContractNumber": "number",
        "nihICsSupportingStudy": ["study"],
        "nihProgramOfficerName": "name",
        "nihInstitutionCenterSubmission": "NCI",
        "nihGenomicProgramAdministratorName": "Name",
        "multiCenterStudy": true,
        "collaboratingSites": ["site"],
        "controlledAccessRequiredForGenomicSummaryResultsGSR": true,
        "controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation": "explanation",
        "alternativeDataSharingPlan": true,
        "alternativeDataSharingPlanReasons": ["Legal Restrictions"],
        "alternativeDataSharingPlanExplanation": "explanation",
        "alternativeDataSharingPlanFileName": "file name",
        "alternativeDataSharingPlanDataSubmitted": "Within 3 months of the last data generated or last clinical visit",
        "alternativeDataSharingPlanDataReleased": true,
        "alternativeDataSharingPlanTargetDeliveryDate": "date",
        "alternativeDataSharingPlanTargetPublicReleaseDate": "date",
        "alternativeDataSharingPlanAccessManagement": "Open Access",
        "consentGroups": [
          {
            "fileTypes": [{
              "fileType": "Arrays",
              "functionalEquivalence": "equivalence"
            }],
            "hmb": true,
            "consentGroupName": "Name",
            "numberOfParticipants": 2,
            "dataAccessCommitteeId": 1,
            "dataLocation": "AnVIL Workspace",
            "url": "https://asdf.com"
          },
          {
            "datasetId": 1,
            "fileTypes": [{
              "fileType": "Arrays",
              "functionalEquivalence": "equivalence"
            }],
            "numberOfParticipants": 2,
            "dataAccessCommitteeId": 1,
            "dataLocation": "AnVIL Workspace",
            "url": "https://asdf.com"
          }
        ]
      }
      """;

}
