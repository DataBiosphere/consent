package org.broadinstitute.consent.http.models.draft;

import com.google.gson.Gson;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.DraftStudyDataset;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

public class StudyBuilder implements ConsentLogger {

  // Study Fields
  protected static final String ALTERNATIVE_DATA_SHARING_PLAN = "alternativeDataSharingPlan";
  protected static final String ALTERNATIVE_DATA_SHARING_PLAN_REASONS = "alternativeDataSharingPlanReasons";
  protected static final String ALTERNATIVE_DATA_SHARING_PLAN_EXPLANATION = "alternativeDataSharingPlanExplanation";
  protected static final String ALTERNATIVE_DATA_SHARING_PLAN_FILE_NAME = "alternativeDataSharingPlanFileName";
  protected static final String ALTERNATIVE_DATA_SHARING_PLAN_DATA_SUBMITTED = "alternativeDataSharingPlanDataSubmitted";
  protected static final String ALTERNATIVE_DATA_SHARING_PLAN_DATA_RELEASED = "alternativeDataSharingPlanDataReleased";
  protected static final String ALTERNATIVE_DATA_SHARING_PLAN_TARGET_DELIVERY_DATE = "alternativeDataSharingPlanTargetDeliveryDate";
  protected static final String ALTERNATIVE_DATA_SHARING_PLAN_TARGET_PUBLIC_RELEASE_DATE = "alternativeDataSharingPlanTargetPublicReleaseDate";
  protected static final String ALTERNATIVE_DATA_SHARING_PLAN_ACCESS_MANAGEMENT = "alternativeDataSharingPlanAccessManagement";
  protected static final String COLLABORATING_SITES = "collaboratingSites";
  protected static final String CONTROLLED_ACCESS_REQUIRED_FOR_GENOMIC_SUMMARY_RESULTS_GSR = "controlledAccessRequiredForGenomicSummaryResultsGSR";
  protected static final String CONTROLLED_ACCESS_REQUIRED_FOR_GENOMIC_SUMMARY_RESULTS_GSRREQUIRED_EXPLANATION = "controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation";
  protected static final String DATA_CUSTODIAN_EMAIL = "dataCustodianEmail";
  protected static final String DB_GAP_PHS_ID = "dbGaPPhsID";
  protected static final String DB_GAP_STUDY_REGISTRATION_NAME = "dbGaPStudyRegistrationName";
  protected static final String EMBARGO_RELEASE_DATE = "embargoReleaseDate";
  protected static final String MULTI_CENTER_STUDY = "multiCenterStudy";
  protected static final String NIH_ANVIL_USE = "nihAnvilUse";
  protected static final String NIH_GRANT_CONTRACT_NUMBER = "nihGrantContractNumber";
  protected static final String NIH_ICS_SUPPORTING_STUDY = "nihICsSupportingStudy";
  protected static final String NIH_PROGRAM_OFFICER_NAME = "nihProgramOfficerName";
  protected static final String NIH_INSTITUTION_CENTER_SUBMISSION = "nihInstitutionCenterSubmission";
  protected static final String NIH_GENOMIC_PROGRAM_ADMINISTRATOR_NAME = "nihGenomicProgramAdministratorName";
  protected static final String PHENOTYPE_INDICATION = "phenotypeIndication";
  protected static final String PI_INSTITUTION = "piInstitution";
  protected static final String SEQUENCING_CENTER = "sequencingCenter";
  protected static final String SPECIES = "species";
  protected static final String STUDY_TYPE = "studyType";
  protected static final String SUBMITTING_TO_ANVIL = "submittingToAnvil";
  // Dataset Fields
  protected static final String ACCESS_MANAGEMENT = "accessManagement";
  protected static final String DATA_LOCATION = "dataLocation";
  protected static final String URL = "url";
  protected static final String NUMBER_OF_PARTICIPANTS = "numberOfParticipants";
  protected static final String FILE_TYPES = "fileTypes";

  protected static final Gson gson = GsonUtil.getInstance();

  public Study buildStudyFromDraftLegacyRegistration(DraftStudyDataset draft) {
    // Set draft related fields
    Study study = new Study();
    study.setCreateDate(draft.getCreateDate());
    study.setUpdateDate(draft.getUpdateDate());
    study.setName(draft.getName());
    study.setUuid(draft.getUUID());
    study.setCreateUserId(draft.getCreateUser().getUserId());
    study.setCreateUserEmail(draft.getCreateUser().getEmail());
    study.setUpdateUserId(draft.getUpdateUser().getUserId());
    draft.getStoredFiles()
        .stream()
        .filter(f -> f.getCategory().equals(FileCategory.ALTERNATIVE_DATA_SHARING_PLAN))
        .findFirst()
        .ifPresent(study::setAlternativeDataSharingPlan);
    // Set legacy registration related fields
    LegacyRegistration lr = gson.fromJson(draft.getJson(), LegacyRegistration.class);
    study.setDescription(lr.studyDescription());
    study.setDataTypes(lr.dataTypes());
    study.setPublicVisibility(lr.publicVisibility());
    study.setPiName(lr.piName());

    if (lr.studyId() != null) {
      study.setStudyId(lr.studyId());
    }
    if (lr.phenotypeIndication() != null) {
      study.addProperty(
          new StudyProperty(PHENOTYPE_INDICATION, lr.phenotypeIndication(), PropertyType.String));
    }
    if (lr.studyType() != null) {
      study.addProperty(new StudyProperty(STUDY_TYPE, lr.studyType(), PropertyType.String));
    }
    if (lr.species() != null) {
      study.addProperty(new StudyProperty(SPECIES, lr.species(), PropertyType.String));
    }
    // TODO: dataSubmitterUserId
    if (lr.dataCustodianEmail() != null) {
      study.addProperty(
          new StudyProperty(DATA_CUSTODIAN_EMAIL, lr.dataCustodianEmail(), PropertyType.Json));
    }
    if (lr.nihAnvilUse() != null) {
      study.addProperty(
          new StudyProperty(NIH_ANVIL_USE, lr.nihAnvilUse(), PropertyType.String));
    }
    if (lr.submittingToAnvil() != null) {
      study.addProperty(
          new StudyProperty(SUBMITTING_TO_ANVIL, lr.submittingToAnvil(), PropertyType.Boolean));
    }
    if (lr.dbGaPPhsID() != null) {
      study.addProperty(new StudyProperty(DB_GAP_PHS_ID, lr.dbGaPPhsID(), PropertyType.String));
    }
    if (lr.dbGaPStudyRegistrationName() != null) {
      study.addProperty(
          new StudyProperty(DB_GAP_STUDY_REGISTRATION_NAME, lr.dbGaPStudyRegistrationName(),
              PropertyType.String));
    }
    if (lr.embargoReleaseDate() != null) {
      study.addProperty(
          new StudyProperty(EMBARGO_RELEASE_DATE, lr.embargoReleaseDate(), PropertyType.String));
    }
    if (lr.sequencingCenter() != null) {
      study.addProperty(
          new StudyProperty(SEQUENCING_CENTER, lr.sequencingCenter(), PropertyType.String));
    }
    if (lr.piInstitution() != null) {
      study.addProperty(new StudyProperty(PI_INSTITUTION, lr.piInstitution(), PropertyType.Number));
    }
    if (lr.nihGrantContractNumber() != null) {
      study.addProperty(new StudyProperty(NIH_GRANT_CONTRACT_NUMBER, lr.nihGrantContractNumber(),
          PropertyType.String));
    }
    if (lr.nihICsSupportingStudy() != null) {
      study.addProperty(new StudyProperty(NIH_ICS_SUPPORTING_STUDY, lr.nihICsSupportingStudy(),
          PropertyType.Json));
    }
    if (lr.nihProgramOfficerName() != null) {
      study.addProperty(new StudyProperty(NIH_PROGRAM_OFFICER_NAME, lr.nihProgramOfficerName(),
          PropertyType.String));
    }
    if (lr.nihInstitutionCenterSubmission() != null) {
      study.addProperty(new StudyProperty(NIH_INSTITUTION_CENTER_SUBMISSION,
          lr.nihInstitutionCenterSubmission(), PropertyType.String));
    }
    if (lr.nihGenomicProgramAdministratorName() != null) {
      study.addProperty(new StudyProperty(NIH_GENOMIC_PROGRAM_ADMINISTRATOR_NAME,
          lr.nihGenomicProgramAdministratorName(), PropertyType.String));
    }
    if (lr.multiCenterStudy() != null) {
      study.addProperty(
          new StudyProperty(MULTI_CENTER_STUDY, lr.multiCenterStudy(), PropertyType.Boolean));
    }
    if (lr.collaboratingSites() != null) {
      study.addProperty(
          new StudyProperty(COLLABORATING_SITES, lr.collaboratingSites(), PropertyType.Json));
    }
    if (lr.controlledAccessRequiredForGenomicSummaryResultsGSR() != null) {
      study.addProperty(
          new StudyProperty(CONTROLLED_ACCESS_REQUIRED_FOR_GENOMIC_SUMMARY_RESULTS_GSR,
              lr.controlledAccessRequiredForGenomicSummaryResultsGSR(), PropertyType.Boolean));
    }
    if (lr.controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation() != null) {
      study.addProperty(new StudyProperty(
          CONTROLLED_ACCESS_REQUIRED_FOR_GENOMIC_SUMMARY_RESULTS_GSRREQUIRED_EXPLANATION,
          lr.controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation(),
          PropertyType.String));
    }
    if (lr.alternativeDataSharingPlan() != null) {
      study.addProperty(new StudyProperty(ALTERNATIVE_DATA_SHARING_PLAN, lr.alternativeDataSharingPlan(),
          PropertyType.Boolean));
    }
    if (lr.alternativeDataSharingPlanReasons() != null) {
      study.addProperty(new StudyProperty(ALTERNATIVE_DATA_SHARING_PLAN_REASONS,
          lr.alternativeDataSharingPlanReasons(), PropertyType.Json));
    }
    if (lr.alternativeDataSharingPlanExplanation() != null) {
      study.addProperty(new StudyProperty(ALTERNATIVE_DATA_SHARING_PLAN_EXPLANATION,
          lr.alternativeDataSharingPlanExplanation(), PropertyType.String));
    }
    if (lr.alternativeDataSharingPlanFileName() != null) {
      study.addProperty(new StudyProperty(ALTERNATIVE_DATA_SHARING_PLAN_FILE_NAME,
          lr.alternativeDataSharingPlanFileName(), PropertyType.String));
    }
    if (lr.alternativeDataSharingPlanDataSubmitted() != null) {
      study.addProperty(new StudyProperty(ALTERNATIVE_DATA_SHARING_PLAN_DATA_SUBMITTED,
          lr.alternativeDataSharingPlanDataSubmitted(), PropertyType.String));
    }
    if (lr.alternativeDataSharingPlanDataReleased() != null) {
      study.addProperty(new StudyProperty(ALTERNATIVE_DATA_SHARING_PLAN_DATA_RELEASED,
          lr.alternativeDataSharingPlanDataReleased(), PropertyType.Boolean));
    }
    if (lr.alternativeDataSharingPlanTargetDeliveryDate() != null) {
      study.addProperty(new StudyProperty(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_DELIVERY_DATE,
          lr.alternativeDataSharingPlanTargetDeliveryDate(), PropertyType.String));
    }
    if (lr.alternativeDataSharingPlanTargetPublicReleaseDate() != null) {
      study.addProperty(new StudyProperty(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_PUBLIC_RELEASE_DATE,
          lr.alternativeDataSharingPlanTargetPublicReleaseDate(), PropertyType.String));
    }
    if (lr.alternativeDataSharingPlanAccessManagement() != null) {
      study.addProperty(new StudyProperty(ALTERNATIVE_DATA_SHARING_PLAN_ACCESS_MANAGEMENT,
          lr.alternativeDataSharingPlanAccessManagement(), PropertyType.String));
    }
    if (lr.consentGroups() != null) {
      study.addDatasets(getDatasetsFromConsentGroups(lr.consentGroups()));
    }
    return study;
  }

  public List<Dataset> getDatasetsFromConsentGroups(List<ConsentGroup> consentGroups) {
    if (consentGroups != null) {
      return consentGroups.stream().map(this::mapConsentGroupObjectToV1SchemaDataset).toList();
    }
    return List.of();
  }

  protected Dataset mapConsentGroupObjectToV1SchemaDataset(ConsentGroup cg) {
    Dataset dataset = new Dataset();
    dataset.setName(cg.consentGroupName());
    dataset.setDacId(cg.dataAccessCommitteeId());
    if (cg.datasetIdentifier() != null) {
      dataset.setAlias(Dataset.parseIdentifierToAlias(cg.datasetIdentifier()));
    }
    if (cg.accessManagement() != null) {
      dataset.addProperty(createDatasetProp(ACCESS_MANAGEMENT, cg.accessManagement(), PropertyType.String));
    }
    if (cg.dataLocation() != null) {
      dataset.addProperty(createDatasetProp(DATA_LOCATION, cg.dataLocation().value(), PropertyType.String));
    }
    if (cg.url() != null) {
      dataset.addProperty(createDatasetProp(URL, cg.url(), PropertyType.String));
    }
    if (cg.numberOfParticipants() != null) {
      dataset.addProperty(createDatasetProp(NUMBER_OF_PARTICIPANTS, cg.numberOfParticipants(), PropertyType.Number));
    }
    DataUse dataUse = new DataUse();
    if (cg.generalResearchUse() != null && cg.generalResearchUse()) {
      dataUse.setGeneralUse(true);
    }
    if (cg.hmb() != null && cg.hmb()) {
      dataUse.setHmbResearch(true);
    }
    if (cg.diseaseSpecificUse() != null && !cg.diseaseSpecificUse().isEmpty()) {
      dataUse.setDiseaseRestrictions(cg.diseaseSpecificUse());
    }
    if (cg.poa() != null && cg.poa()) {
      dataUse.setPopulationOriginsAncestry(true);
    }
    if (cg.otherPrimary() != null) {
      dataUse.setOther(cg.otherPrimary());
    }
    if (cg.nmds() != null && cg.nmds()) {
      dataUse.setMethodsResearch(true);
    }
    if (cg.gso() != null && cg.gso()) {
      dataUse.setGeneticStudiesOnly(true);
    }
    if (cg.pub() != null && cg.pub()) {
      dataUse.setPublicationResults(true);
    }
    if (cg.col() != null && cg.col()) {
      dataUse.setCollaboratorRequired(true);
    }
    if (cg.irb() != null && cg.irb()) {
      dataUse.setEthicsApprovalRequired(true);
    }
    if (cg.gs() != null) {
      dataUse.setGeographicalRestrictions(cg.gs());
    }
    if (cg.mor() != null && cg.mor()) {
      if (cg.morDate() != null) {
        dataUse.setPublicationMoratorium(cg.morDate());
      }
    }
    if (cg.npu() != null && cg.npu()) {
      dataUse.setNonProfitUse(true);
    }
    if (cg.otherSecondary() != null) {
      dataUse.setSecondaryOther(cg.otherSecondary());
    }
    if (cg.fileTypes() != null && !cg.fileTypes().isEmpty()) {
      dataset.addProperty(createDatasetProp(FILE_TYPES, cg.fileTypes(), PropertyType.Json));
    }
    dataset.setDataUse(dataUse);
    return dataset;
  }

  protected DatasetProperty createDatasetProp(String name, Object value, PropertyType type) {
    DatasetProperty prop = new DatasetProperty();
    prop.setPropertyName(name);
    prop.setPropertyValue(value);
    prop.setPropertyType(type);
    return prop;
  }

}
