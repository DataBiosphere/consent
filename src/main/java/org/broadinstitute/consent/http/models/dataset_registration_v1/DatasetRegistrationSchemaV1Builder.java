package org.broadinstitute.consent.http.models.dataset_registration_v1;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.DataLocation;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.AlternativeDataSharingPlanControlledOpenAccess;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.AlternativeDataSharingPlanDataSubmitted;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.NihAnvilUse;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.NihInstitutionCenterSubmission;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

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
  public static final String alternativeDataSharingPlanControlledOpenAccess = "alternativeDataSharingPlanControlledOpenAccess";

  public DatasetRegistrationSchemaV1 build(Study study, List<Dataset> datasets) {
    DatasetRegistrationSchemaV1 schemaV1 = new DatasetRegistrationSchemaV1();
    if (Objects.nonNull(study)) {
      schemaV1.setStudyId(study.getStudyId());
      schemaV1.setStudyName(study.getName());
      String studyTypeVal = findStringPropValue(study.getProperties(), studyType);
      if (Objects.nonNull(studyTypeVal)) {
        schemaV1.setStudyType(DatasetRegistrationSchemaV1.StudyType.fromValue(studyTypeVal));
      }
      schemaV1.setStudyDescription(study.getDescription());
      schemaV1.setDataTypes(study.getDataTypes());
      schemaV1.setPhenotypeIndication(findStringPropValue(study.getProperties(), phenotypeIndication));
      schemaV1.setSpecies(findStringPropValue(study.getProperties(), species));
      schemaV1.setPiName(study.getPiName());
      schemaV1.setDataSubmitterUserId(study.getCreateUserId());
      schemaV1.setDataCustodianEmail(findListStringPropValue(study.getProperties(), dataCustodianEmail));
      schemaV1.setPublicVisibility(study.getPublicVisibility());
      String nihAnvilUseVal = findStringPropValue(study.getProperties(), nihAnvilUse);
      if (Objects.nonNull(nihAnvilUseVal)) {
        schemaV1.setNihAnvilUse(NihAnvilUse.fromValue(nihAnvilUseVal));
      }
      schemaV1.setSubmittingToAnvil(findBooleanPropValue(study.getProperties(), submittingToAnvil));
      schemaV1.setDbGaPPhsID(findStringPropValue(study.getProperties(), dbGaPPhsID));
      schemaV1.setDbGaPStudyRegistrationName(findStringPropValue(study.getProperties(), dbGaPStudyRegistrationName));
      schemaV1.setEmbargoReleaseDate(findStringPropValue(study.getProperties(), embargoReleaseDate));
      schemaV1.setSequencingCenter(findStringPropValue(study.getProperties(), sequencingCenter));
      schemaV1.setPiInstitution(findIntegerPropValue(study.getProperties(), piInstitution));
      schemaV1.setNihGrantContractNumber(findStringPropValue(study.getProperties(), nihGrantContractNumber));
      schemaV1.setNihICsSupportingStudy(findListNICSSPropValue(study.getProperties()));
      schemaV1.setNihProgramOfficerName(findStringPropValue(study.getProperties(), nihProgramOfficerName));
      String nihInstitutionCenterSubmissionVal = findStringPropValue(study.getProperties(), nihInstitutionCenterSubmission);
      if (Objects.nonNull(nihInstitutionCenterSubmissionVal)) {
        schemaV1.setNihInstitutionCenterSubmission(NihInstitutionCenterSubmission.fromValue(nihInstitutionCenterSubmissionVal));
      }
      schemaV1.setNihGenomicProgramAdministratorName(findStringPropValue(study.getProperties(), nihGenomicProgramAdministratorName));
      schemaV1.setMultiCenterStudy(findBooleanPropValue(study.getProperties(), multiCenterStudy));
      schemaV1.setCollaboratingSites(findListStringPropValue(study.getProperties(), collaboratingSites));
      schemaV1.setControlledAccessRequiredForGenomicSummaryResultsGSR(findBooleanPropValue(study.getProperties(), controlledAccessRequiredForGenomicSummaryResultsGSR));
      schemaV1.setControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation(findStringPropValue(study.getProperties(), controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation));
      if (Objects.nonNull(study.getAlternativeDataSharingPlan())) {
        schemaV1.setAlternativeDataSharingPlan(Boolean.TRUE);
      }
      schemaV1.setAlternativeDataSharingPlanReasons(findListADSPRPropValue(study.getProperties()));
      schemaV1.setAlternativeDataSharingPlanExplanation(findStringPropValue(study.getProperties(), alternativeDataSharingPlanExplanation));
      schemaV1.setAlternativeDataSharingPlanFileName(findStringPropValue(study.getProperties(), alternativeDataSharingPlanFileName));
      String alternativeDataSharingPlanDataSubmittedVal = findStringPropValue(study.getProperties(), alternativeDataSharingPlanDataSubmitted);
      if (Objects.nonNull(alternativeDataSharingPlanDataSubmittedVal)) {
        schemaV1.setAlternativeDataSharingPlanDataSubmitted(AlternativeDataSharingPlanDataSubmitted.fromValue(alternativeDataSharingPlanDataSubmittedVal));
      }
      schemaV1.setAlternativeDataSharingPlanDataReleased(findBooleanPropValue(study.getProperties(), alternativeDataSharingPlanDataReleased));
      schemaV1.setAlternativeDataSharingPlanTargetDeliveryDate(findStringPropValue(study.getProperties(), alternativeDataSharingPlanTargetDeliveryDate));
      schemaV1.setAlternativeDataSharingPlanTargetPublicReleaseDate(findStringPropValue(study.getProperties(), alternativeDataSharingPlanTargetPublicReleaseDate));
      String alternativeDataSharingPlanControlledOpenAccessVal = findStringPropValue(study.getProperties(), alternativeDataSharingPlanControlledOpenAccess);
      if (Objects.nonNull(alternativeDataSharingPlanControlledOpenAccessVal)) {
        schemaV1.setAlternativeDataSharingPlanControlledOpenAccess(AlternativeDataSharingPlanControlledOpenAccess.fromValue(alternativeDataSharingPlanControlledOpenAccessVal));
      }
    }
    if (!datasets.isEmpty()) {
      if (Objects.isNull(schemaV1.getConsentGroups())) {
        schemaV1.setConsentGroups(new ArrayList<>());
      }
      List<ConsentGroup> consentGroups = datasets.stream().filter(Objects::nonNull).map(this::consentGroupFromDataset).toList();
      if (!consentGroups.isEmpty()) {
        schemaV1.getConsentGroups().addAll(consentGroups);
      }
    }
    return schemaV1;
  }

  private List<String> findListStringPropValue(Set<StudyProperty> props, String key) {
    if (Objects.nonNull(props) && !props.isEmpty()) {
      return props
          .stream()
          .filter(p -> p.getKey().equalsIgnoreCase(key))
          .map(StudyProperty::getValue)
          .map(p -> GsonUtil.getInstance().fromJson(p.toString(), JsonElement.class))
          .map(JsonElement::getAsJsonArray)
          .map(JsonArray::asList)
          .flatMap(List::stream)
          .map(JsonElement::getAsString)
          .toList();
    }
    return List.of();
  }

  private List<AlternativeDataSharingPlanReason> findListADSPRPropValue(Set<StudyProperty> props) {
    if (Objects.nonNull(props) && !props.isEmpty()) {
      return props
          .stream()
          .filter(p -> p.getKey().equalsIgnoreCase(alternativeDataSharingPlanReasons))
          .map(StudyProperty::getValue)
          .map(p -> GsonUtil.getInstance().fromJson(p.toString(), JsonElement.class))
          .map(JsonElement::getAsJsonArray)
          .map(JsonArray::asList)
          .flatMap(List::stream)
          .map(JsonElement::getAsString)
          .map(AlternativeDataSharingPlanReason::fromValue)
          .toList();
    }
    return List.of();
  }

  private List<NihICsSupportingStudy> findListNICSSPropValue(Set<StudyProperty> props) {
    if (Objects.nonNull(props) && !props.isEmpty()) {
      return props
          .stream()
          .filter(p -> p.getKey().equalsIgnoreCase(nihICsSupportingStudy))
          .map(StudyProperty::getValue)
          .map(p -> GsonUtil.getInstance().fromJson(p.toString(), JsonElement.class))
          .map(JsonElement::getAsJsonArray)
          .map(JsonArray::asList)
          .flatMap(List::stream)
          .map(JsonElement::getAsString)
          .map(NihICsSupportingStudy::fromValue)
          .toList();
    }
    return List.of();
  }

  private String findStringPropValue(Set<StudyProperty> props, String propName) {
    if (Objects.nonNull(props) && !props.isEmpty()) {
      return props
          .stream()
          .filter(p -> p.getKey().equalsIgnoreCase(propName))
          .map(StudyProperty::getValue)
          .map(Object::toString)
          .findFirst()
          .orElse(null);
    }
    return null;
  }

  private Boolean findBooleanPropValue(Set<StudyProperty> props, String propName) {
    if (Objects.nonNull(props) && !props.isEmpty()) {
      return props
          .stream()
          .filter(p -> p.getKey().equalsIgnoreCase(propName))
          .map(StudyProperty::getValue)
          .map(Object::toString)
          .map(Boolean::valueOf)
          .findFirst()
          .orElse(null);
    }
    return null;
  }

  private Integer findIntegerPropValue(Set<StudyProperty> props, String propName) {
    if (Objects.nonNull(props) && !props.isEmpty()) {
      return props
          .stream()
          .filter(p -> p.getKey().equalsIgnoreCase(propName))
          .map(StudyProperty::getValue)
          .map(Object::toString)
          .map(Integer::valueOf)
          .findFirst()
          .orElse(null);
    }
    return null;
  }

  public static final String openAccess = "openAccess";
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

  private ConsentGroup consentGroupFromDataset(Dataset dataset) {
    if (Objects.nonNull(dataset)) {
      ConsentGroup consentGroup = new ConsentGroup();
      consentGroup.setDatasetId(dataset.getDataSetId());
      consentGroup.setConsentGroupName(dataset.getName());
      consentGroup.setOpenAccess(findBooleanDSPropValue(dataset.getProperties(), openAccess));
      consentGroup.setGeneralResearchUse(findBooleanDSPropValue(dataset.getProperties(), generalResearchUse));
      consentGroup.setHmb(findBooleanDSPropValue(dataset.getProperties(), hmb));
      consentGroup.setDiseaseSpecificUse(findListStringDSPropValue(dataset.getProperties(), diseaseSpecificUse));
      consentGroup.setPoa(findBooleanDSPropValue(dataset.getProperties(), poa));
      consentGroup.setOtherPrimary(findStringDSPropValue(dataset.getProperties(), otherPrimary));
      consentGroup.setNmds(findBooleanDSPropValue(dataset.getProperties(), nmds));
      consentGroup.setGso(findBooleanDSPropValue(dataset.getProperties(), gso));
      consentGroup.setPub(findBooleanDSPropValue(dataset.getProperties(), pub));
      consentGroup.setCol(findBooleanDSPropValue(dataset.getProperties(), col));
      consentGroup.setIrb(findBooleanDSPropValue(dataset.getProperties(), irb));
      consentGroup.setGs(findStringDSPropValue(dataset.getProperties(), gs));
      consentGroup.setMor(findBooleanDSPropValue(dataset.getProperties(), mor));
      consentGroup.setMorDate(findStringDSPropValue(dataset.getProperties(), morDate));
      consentGroup.setNpu(findBooleanDSPropValue(dataset.getProperties(), npu));
      consentGroup.setOtherSecondary(findStringDSPropValue(dataset.getProperties(), otherSecondary));
      consentGroup.setDataAccessCommitteeId(findIntegerDSPropValue(dataset.getProperties(), dataAccessCommitteeId));
      String dataLocationVal = findStringDSPropValue(dataset.getProperties(), dataLocation);
      if (Objects.nonNull(dataLocationVal)) {
        consentGroup.setDataLocation(DataLocation.fromValue(dataLocationVal));
      }
      consentGroup.setUrl(findStringDSPropValue(dataset.getProperties(), url));
      consentGroup.setNumberOfParticipants(findIntegerDSPropValue(dataset.getProperties(), numberOfParticipants));
      consentGroup.setFileTypes(findListFTSODSPropValue(dataset.getProperties()));
      return consentGroup;
    }
    return null;
  }

  private String findStringDSPropValue(Set<DatasetProperty> props, String propName) {
    if (Objects.nonNull(props) && !props.isEmpty()) {
      return props
          .stream()
          .filter(p -> p.getSchemaProperty().equalsIgnoreCase(propName))
          .map(DatasetProperty::getPropertyValueAsString)
          .findFirst()
          .orElse(null);
    }
    return null;
  }

  private Boolean findBooleanDSPropValue(Set<DatasetProperty> props, String propName) {
    if (Objects.nonNull(props) && !props.isEmpty()) {
      return props
          .stream()
          .filter(p -> p.getSchemaProperty().equalsIgnoreCase(propName))
          .map(DatasetProperty::getPropertyValue)
          .map(Object::toString)
          .map(Boolean::valueOf)
          .findFirst()
          .orElse(null);
    }
    return null;
  }

  private List<String> findListStringDSPropValue(Set<DatasetProperty> props, String key) {
    if (Objects.nonNull(props) && !props.isEmpty()) {
      return props
          .stream()
          .filter(p -> p.getSchemaProperty().equalsIgnoreCase(key))
          .map(DatasetProperty::getPropertyValue)
          .map(p -> GsonUtil.getInstance().fromJson(p.toString(), JsonElement.class))
          .map(JsonElement::getAsJsonArray)
          .map(JsonArray::asList)
          .flatMap(List::stream)
          .map(JsonElement::getAsString)
          .toList();
    }
    return List.of();
  }

  private Integer findIntegerDSPropValue(Set<DatasetProperty> props, String propName) {
    if (Objects.nonNull(props) && !props.isEmpty()) {
      return props
          .stream()
          .filter(p -> p.getSchemaProperty().equalsIgnoreCase(propName))
          .map(DatasetProperty::getPropertyValue)
          .map(Object::toString)
          .map(Integer::valueOf)
          .findFirst()
          .orElse(null);
    }
    return null;
  }

  private List<FileTypeObject> findListFTSODSPropValue(Set<DatasetProperty> props) {
    if (Objects.nonNull(props) && !props.isEmpty()) {
      return props
          .stream()
          .filter(p -> p.getSchemaProperty().equalsIgnoreCase(fileTypes))
          .map(DatasetProperty::getPropertyValueAsString)
          .map(p -> GsonUtil.getInstance().fromJson(p, JsonElement.class))
          .map(JsonElement::getAsJsonArray)
          .map(JsonArray::asList)
          .flatMap(List::stream)
          .map(JsonElement::getAsString)
          .map(p -> GsonUtil.getInstance().fromJson(p, FileTypeObject.class))
          .toList();
    }
    return List.of();
  }

}
