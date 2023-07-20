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
        "phenotypeIndication": "",
        "species": "species",
        "piName": "PI Name"
      }
      """;

  public static final String registrationWithDataSubmitterUserId = """
      {
        "studyType": "Observational",
        "studyDescription": "description",
        "dataTypes": ["types"],
        "phenotypeIndication": "",
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
        "phenotypeIndication": "",
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
        "phenotypeIndication": "",
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

}
