package org.broadinstitute.consent.http.models.dataset_registration_v1;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.AlternativeDataSharingPlanControlledOpenAccess;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.AlternativeDataSharingPlanDataSubmitted;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.NihAnvilUse;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.NihInstitutionCenterSubmission;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

public class DatasetRegistrationSchemaV1Builder {

  public DatasetRegistrationSchemaV1 build(Study study, List<Dataset> datasets) {
    DatasetRegistrationSchemaV1 schemaV1 = new DatasetRegistrationSchemaV1();
    if (Objects.nonNull(study)) {
      schemaV1.setStudyId(study.getStudyId());
      schemaV1.setStudyName(study.getName());
      String studyType = findStringPropValue(study.getProperties(), "studyType");
      if (Objects.nonNull(studyType)) {
        schemaV1.setStudyType(DatasetRegistrationSchemaV1.StudyType.fromValue(studyType));
      }
      schemaV1.setStudyDescription(study.getDescription());
      schemaV1.setDataTypes(study.getDataTypes());
      schemaV1.setPhenotypeIndication(findStringPropValue(study.getProperties(), "phenotypeIndication"));
      schemaV1.setSpecies(findStringPropValue(study.getProperties(), "species"));
      schemaV1.setPiName(study.getPiName());
      schemaV1.setDataSubmitterUserId(study.getCreateUserId());
      schemaV1.setDataCustodianEmail(findListStringPropValue(study.getProperties(), "dataCustodianEmail"));
      schemaV1.setPublicVisibility(study.getPublicVisibility());
      String nihAnvilUse = findStringPropValue(study.getProperties(), "nihAnvilUse");
      if (Objects.nonNull(nihAnvilUse)) {
        schemaV1.setNihAnvilUse(NihAnvilUse.valueOf(nihAnvilUse));
      }
      schemaV1.setSubmittingToAnvil(findBooleanPropValue(study.getProperties(), "submittingToAnvil"));
      schemaV1.setDbGaPPhsID(findStringPropValue(study.getProperties(), "dbGaPPhsID"));
      schemaV1.setDbGaPStudyRegistrationName(findStringPropValue(study.getProperties(), "dbGaPStudyRegistrationName"));
      schemaV1.setEmbargoReleaseDate(findStringPropValue(study.getProperties(), "embargoReleaseDate"));
      schemaV1.setSequencingCenter(findStringPropValue(study.getProperties(), "sequencingCenter"));
      schemaV1.setPiInstitution(findIntegerPropValue(study.getProperties(), "piInstitution"));
      schemaV1.setNihGrantContractNumber(findStringPropValue(study.getProperties(), "nihGrantContractNumber"));
      schemaV1.setNihICsSupportingStudy(findListNICSSPropValue(study.getProperties()));
      schemaV1.setNihProgramOfficerName(findStringPropValue(study.getProperties(), "nihProgramOfficerName"));
      String nihInstitutionCenterSubmission = findStringPropValue(study.getProperties(), "nihInstitutionCenterSubmission");
      if (Objects.nonNull(nihInstitutionCenterSubmission)) {
        schemaV1.setNihInstitutionCenterSubmission(NihInstitutionCenterSubmission.valueOf(nihInstitutionCenterSubmission));
      }
      schemaV1.setNihGenomicProgramAdministratorName(findStringPropValue(study.getProperties(), "nihGenomicProgramAdministratorName"));
      schemaV1.setMultiCenterStudy(findBooleanPropValue(study.getProperties(), "multiCenterStudy"));
      schemaV1.setCollaboratingSites(findListStringPropValue(study.getProperties(), "collaboratingSites"));
      schemaV1.setControlledAccessRequiredForGenomicSummaryResultsGSR(findBooleanPropValue(study.getProperties(), "controlledAccessRequiredForGenomicSummaryResultsGSR"));
      schemaV1.setControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation(findStringPropValue(study.getProperties(), "controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation"));
      if (Objects.nonNull(study.getAlternativeDataSharingPlan())) {
        schemaV1.setAlternativeDataSharingPlan(Boolean.TRUE);
      }
      schemaV1.setAlternativeDataSharingPlanReasons(findListADSPRPropValue(study.getProperties()));
      schemaV1.setAlternativeDataSharingPlanExplanation(findStringPropValue(study.getProperties(), "alternativeDataSharingPlanExplanation"));
      schemaV1.setAlternativeDataSharingPlanFileName(findStringPropValue(study.getProperties(), "alternativeDataSharingPlanFileName"));
      String alternativeDataSharingPlanDataSubmitted = findStringPropValue(study.getProperties(), "alternativeDataSharingPlanDataSubmitted");
      if (Objects.nonNull(alternativeDataSharingPlanDataSubmitted)) {
        schemaV1.setAlternativeDataSharingPlanDataSubmitted(AlternativeDataSharingPlanDataSubmitted.fromValue(alternativeDataSharingPlanDataSubmitted));
      }
      schemaV1.setAlternativeDataSharingPlanDataReleased(findBooleanPropValue(study.getProperties(), "alternativeDataSharingPlanDataReleased"));
      schemaV1.setAlternativeDataSharingPlanTargetDeliveryDate(findStringPropValue(study.getProperties(), "alternativeDataSharingPlanTargetDeliveryDate"));
      schemaV1.setAlternativeDataSharingPlanTargetPublicReleaseDate(findStringPropValue(study.getProperties(), "alternativeDataSharingPlanTargetPublicReleaseDate"));
      String alternativeDataSharingPlanControlledOpenAccess = findStringPropValue(study.getProperties(), "alternativeDataSharingPlanControlledOpenAccess");
      if (Objects.nonNull(alternativeDataSharingPlanControlledOpenAccess)) {
        schemaV1.setAlternativeDataSharingPlanControlledOpenAccess(AlternativeDataSharingPlanControlledOpenAccess.fromValue(alternativeDataSharingPlanControlledOpenAccess));
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
          .map(p -> GsonUtil.getInstance().toJson(p, List.class))
          .toList();
    }
    return List.of();
  }

  private List<AlternativeDataSharingPlanReason> findListADSPRPropValue(Set<StudyProperty> props) {
    if (Objects.nonNull(props) && !props.isEmpty()) {
      return props
          .stream()
          .filter(p -> p.getKey().equalsIgnoreCase("alternativeDataSharingPlanReasons"))
          .map(StudyProperty::getValue)
          .map(p -> GsonUtil.getInstance().toJson(p, List.class))
          .map(AlternativeDataSharingPlanReason::valueOf)
          .toList();
    }
    return List.of();
  }

  private List<NihICsSupportingStudy> findListNICSSPropValue(Set<StudyProperty> props) {
    if (Objects.nonNull(props) && !props.isEmpty()) {
      return props
          .stream()
          .filter(p -> p.getKey().equalsIgnoreCase("nihICsSupportingStudy"))
          .map(StudyProperty::getValue)
          .map(p -> GsonUtil.getInstance().toJson(p, List.class))
          .map(NihICsSupportingStudy::valueOf)
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

  private ConsentGroup consentGroupFromDataset(Dataset dataset) {
    return new ConsentGroup();
  }

}
