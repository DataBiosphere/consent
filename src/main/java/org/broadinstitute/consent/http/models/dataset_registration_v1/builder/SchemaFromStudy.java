package org.broadinstitute.consent.http.models.dataset_registration_v1.builder;

import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.alternativeDataSharingPlanAccessManagement;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.alternativeDataSharingPlanDataReleased;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.alternativeDataSharingPlanDataSubmitted;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.alternativeDataSharingPlanExplanation;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.alternativeDataSharingPlanFileName;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.alternativeDataSharingPlanReasons;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.alternativeDataSharingPlanTargetDeliveryDate;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.alternativeDataSharingPlanTargetPublicReleaseDate;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.collaboratingSites;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.controlledAccessRequiredForGenomicSummaryResultsGSR;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.dataCustodianEmail;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.dbGaPPhsID;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.dbGaPStudyRegistrationName;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.embargoReleaseDate;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.multiCenterStudy;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.nihAnvilUse;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.nihGenomicProgramAdministratorName;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.nihGrantContractNumber;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.nihICsSupportingStudy;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.nihInstitutionCenterSubmission;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.nihProgramOfficerName;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.phenotypeIndication;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.piInstitution;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.sequencingCenter;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.species;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.studyType;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.submittingToAnvil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.dataset_registration_v1.AlternativeDataSharingPlanReason;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.AlternativeDataSharingPlanAccessManagement;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.AlternativeDataSharingPlanDataSubmitted;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.NihAnvilUse;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.NihInstitutionCenterSubmission;
import org.broadinstitute.consent.http.models.dataset_registration_v1.NihICsSupportingStudy;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

public class SchemaFromStudy {

  public DatasetRegistrationSchemaV1 build(Study study) {
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
      schemaV1.setPhenotypeIndication(
          findStringPropValue(study.getProperties(), phenotypeIndication));
      schemaV1.setSpecies(findStringPropValue(study.getProperties(), species));
      schemaV1.setPiName(study.getPiName());
      schemaV1.setDataSubmitterUserId(study.getCreateUserId());
      schemaV1.setDataCustodianEmail(
          findListStringPropValue(study.getProperties(), dataCustodianEmail));
      schemaV1.setPublicVisibility(study.getPublicVisibility());
      String nihAnvilUseVal = findStringPropValue(study.getProperties(), nihAnvilUse);
      if (Objects.nonNull(nihAnvilUseVal)) {
        schemaV1.setNihAnvilUse(NihAnvilUse.fromValue(nihAnvilUseVal));
      }
      schemaV1.setSubmittingToAnvil(findBooleanPropValue(study.getProperties(), submittingToAnvil));
      schemaV1.setDbGaPPhsID(findStringPropValue(study.getProperties(), dbGaPPhsID));
      schemaV1.setDbGaPStudyRegistrationName(
          findStringPropValue(study.getProperties(), dbGaPStudyRegistrationName));
      schemaV1.setEmbargoReleaseDate(
          findStringPropValue(study.getProperties(), embargoReleaseDate));
      schemaV1.setSequencingCenter(findStringPropValue(study.getProperties(), sequencingCenter));
      schemaV1.setPiInstitution(findIntegerPropValue(study.getProperties(), piInstitution));
      schemaV1.setNihGrantContractNumber(
          findStringPropValue(study.getProperties(), nihGrantContractNumber));
      schemaV1.setNihICsSupportingStudy(findListNICSSPropValue(study.getProperties()));
      schemaV1.setNihProgramOfficerName(
          findStringPropValue(study.getProperties(), nihProgramOfficerName));
      String nihInstitutionCenterSubmissionVal = findStringPropValue(study.getProperties(),
          nihInstitutionCenterSubmission);
      if (Objects.nonNull(nihInstitutionCenterSubmissionVal)) {
        schemaV1.setNihInstitutionCenterSubmission(
            NihInstitutionCenterSubmission.fromValue(nihInstitutionCenterSubmissionVal));
      }
      schemaV1.setNihGenomicProgramAdministratorName(
          findStringPropValue(study.getProperties(), nihGenomicProgramAdministratorName));
      schemaV1.setMultiCenterStudy(findBooleanPropValue(study.getProperties(), multiCenterStudy));
      schemaV1.setCollaboratingSites(
          findListStringPropValue(study.getProperties(), collaboratingSites));
      schemaV1.setControlledAccessRequiredForGenomicSummaryResultsGSR(
          findBooleanPropValue(study.getProperties(),
              controlledAccessRequiredForGenomicSummaryResultsGSR));
      schemaV1.setControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation(
          findStringPropValue(study.getProperties(),
              controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation));
      if (Objects.nonNull(study.getAlternativeDataSharingPlan())) {
        schemaV1.setAlternativeDataSharingPlan(Boolean.TRUE);
      }
      schemaV1.setAlternativeDataSharingPlanReasons(findListADSPRPropValue(study.getProperties()));
      schemaV1.setAlternativeDataSharingPlanExplanation(
          findStringPropValue(study.getProperties(), alternativeDataSharingPlanExplanation));
      schemaV1.setAlternativeDataSharingPlanFileName(
          findStringPropValue(study.getProperties(), alternativeDataSharingPlanFileName));
      String alternativeDataSharingPlanDataSubmittedVal = findStringPropValue(study.getProperties(),
          alternativeDataSharingPlanDataSubmitted);
      if (Objects.nonNull(alternativeDataSharingPlanDataSubmittedVal)) {
        schemaV1.setAlternativeDataSharingPlanDataSubmitted(
            AlternativeDataSharingPlanDataSubmitted.fromValue(
                alternativeDataSharingPlanDataSubmittedVal));
      }
      schemaV1.setAlternativeDataSharingPlanDataReleased(
          findBooleanPropValue(study.getProperties(), alternativeDataSharingPlanDataReleased));
      schemaV1.setAlternativeDataSharingPlanTargetDeliveryDate(
          findStringPropValue(study.getProperties(), alternativeDataSharingPlanTargetDeliveryDate));
      schemaV1.setAlternativeDataSharingPlanTargetPublicReleaseDate(
          findStringPropValue(study.getProperties(),
              alternativeDataSharingPlanTargetPublicReleaseDate));
      String alternativeDataSharingPlanAccessManagementVal = findStringPropValue(
          study.getProperties(), alternativeDataSharingPlanAccessManagement);
      if (Objects.nonNull(alternativeDataSharingPlanAccessManagementVal)) {
        schemaV1.setAlternativeDataSharingPlanAccessManagement(
            AlternativeDataSharingPlanAccessManagement.fromValue(
                alternativeDataSharingPlanAccessManagementVal));
      }
    }

    return schemaV1;
  }


  @Nullable
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
    return null;
  }

  @Nullable
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
    return null;
  }

  @Nullable
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
    return null;
  }

  @Nullable
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

  @Nullable
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

}
