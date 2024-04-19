package org.broadinstitute.consent.http.models.dataset_registration_v1.builder;

import java.util.List;
import java.util.Objects;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;

public class DatasetRegistrationSchemaV1Builder {

  public static final String studyType = "studyType";
  public static final String phenotypeIndication = "phenotypeIndication";
  public static final String dataCustodianEmail = "dataCustodianEmail";
  public static final String species = "species";
  public static final String nihAnvilUse = "nihAnvilUse";
  public static final String submittingToAnvil = "submittingToAnvil";
  public static final String dbGaPPhsID = "dbGaPPhsID";
  public static final String dbGaPStudyRegistrationName = "dbGaPStudyRegistrationName";
  public static final String embargoReleaseDate = "embargoReleaseDate";
  public static final String sequencingCenter = "sequencingCenter";
  public static final String piInstitution = "piInstitution";
  public static final String nihGrantContractNumber = "nihGrantContractNumber";
  public static final String nihICsSupportingStudy = "nihICsSupportingStudy";
  public static final String nihProgramOfficerName = "nihProgramOfficerName";
  public static final String nihInstitutionCenterSubmission = "nihInstitutionCenterSubmission";
  public static final String nihGenomicProgramAdministratorName = "nihGenomicProgramAdministratorName";
  public static final String multiCenterStudy = "multiCenterStudy";
  public static final String collaboratingSites = "collaboratingSites";
  public static final String controlledAccessRequiredForGenomicSummaryResultsGSR = "controlledAccessRequiredForGenomicSummaryResultsGSR";
  public static final String controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation = "controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation";
  public static final String alternativeDataSharingPlanReasons = "alternativeDataSharingPlanReasons";
  public static final String alternativeDataSharingPlanExplanation = "alternativeDataSharingPlanExplanation";
  public static final String alternativeDataSharingPlanFileName = "alternativeDataSharingPlanFileName";
  public static final String alternativeDataSharingPlanDataSubmitted = "alternativeDataSharingPlanDataSubmitted";
  public static final String alternativeDataSharingPlanDataReleased = "alternativeDataSharingPlanDataReleased";
  public static final String alternativeDataSharingPlanTargetDeliveryDate = "alternativeDataSharingPlanTargetDeliveryDate";
  public static final String alternativeDataSharingPlanTargetPublicReleaseDate = "alternativeDataSharingPlanTargetPublicReleaseDate";
  public static final String alternativeDataSharingPlanAccessManagement = "alternativeDataSharingPlanAccessManagement";
  public static final String accessManagement = "accessManagement";
  public static final String generalResearchUse = "generalResearchUse";
  public static final String hmb = "hmb";
  public static final String diseaseSpecificUse = "diseaseSpecificUse";
  public static final String poa = "poa";
  public static final String otherPrimary = "otherPrimary";
  public static final String nmds = "nmds";
  public static final String gso = "gso";
  public static final String pub = "pub";
  public static final String col = "col";
  public static final String irb = "irb";
  public static final String gs = "gs";
  public static final String mor = "mor";
  public static final String morDate = "morDate";
  public static final String npu = "npu";
  public static final String otherSecondary = "otherSecondary";
  public static final String dataAccessCommitteeId = "dataAccessCommitteeId";
  public static final String dataLocation = "dataLocation";
  public static final String url = "url";
  public static final String numberOfParticipants = "numberOfParticipants";
  public static final String fileTypes = "fileTypes";

  public DatasetRegistrationSchemaV1 build(Study study, List<Dataset> datasets) {
    DatasetRegistrationSchemaV1 schema = new SchemaFromStudy().build(study);
    if (!datasets.isEmpty()) {
      ConsentGroupFromDataset consentGroupFromDataset = new ConsentGroupFromDataset();
      List<ConsentGroup> consentGroups = datasets
        .stream()
        .filter(Objects::nonNull)
        .map(consentGroupFromDataset::build)
        .toList();
      if (!consentGroups.isEmpty()) {
        schema.setConsentGroups(consentGroups);
      }
    }
    return schema;
  }

}
