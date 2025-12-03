package org.broadinstitute.consent.http.models;

import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.StudyType;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

public record StudyPatch(
    String name,
    StudyType studyType,
    String description,
    List<String> dataTypes,
    String phenotypeIndication,
    String species,
    String piName,
    List<String> dataCustodianEmail,
    String alternativeDataSharingPlanTargetDeliveryDate,
    String alternativeDataSharingPlanTargetPublicReleaseDate,
    Boolean publicVisibility) {

  public static final String STUDY_TYPE = "studyType";
  public static final String PHENOTYPE_INDICATION = "phenotypeIndication";
  public static final String SPECIES_TYPE = "species";
  public static final String DATA_CUSTODIAN_EMAIL = "dataCustodianEmail";
  public static final String ALTERNATIVE_DATA_SHARING_PLAN_TARGET_DELIVERY_DATE = "alternativeDataSharingPlanTargetDeliveryDate";
  public static final String ALTERNATIVE_DATA_SHARING_PLAN_TARGET_PUBLIC_RELEASE_DATE = "alternativeDataSharingPlanTargetPublicReleaseDate";

  // Utility method to determine if any patch values differ from the provided Study entity
  public boolean isPatchable(Study study) {
    List<Boolean> checks = new ArrayList<>();
    checks.add(checkName(study));
    checks.add(checkStudyType(study));
    checks.add(checkDescription(study));
    checks.add(checkDataTypes(study));
    checks.add(checkPhenotypeIndication(study));
    checks.add(checkSpecies(study));
    checks.add(checkPiName(study));
    checks.add(checkDataCustodians(study));
    checks.add(checkTargetDate(study));
    checks.add(checkTargetReleaseDate(study));
    checks.add(checkPublicVisibility(study));
    return checks.stream().anyMatch(Boolean::booleanValue);
  }

  private boolean checkName(Study study) {
    return name() != null && !name().equals(study.getName()) && !name().isBlank();
  }

  private boolean checkStudyType(Study study) {
    Optional<StudyProperty> studyTypeProp =
        study.getProperties().stream().filter(p -> p.getKey().equals(STUDY_TYPE)).findFirst();
    if (studyType() != null) {
      return studyTypeProp
          .map(studyProperty -> !studyType().value().equals(studyProperty.getValue()))
          .orElse(true);
    }
    return false;
  }

  private boolean checkDescription(Study study) {
    return description() != null
        && !description().equals(study.getDescription())
        && !description().isBlank();
  }

  private boolean checkDataTypes(Study study) {
    return dataTypes() != null
        && !CollectionUtils.isEqualCollection(dataTypes(), study.getDataTypes());
  }

  private boolean checkPhenotypeIndication(Study study) {
    Optional<StudyProperty> phenoProp =
        study.getProperties().stream()
            .filter(p -> p.getKey().equals(PHENOTYPE_INDICATION))
            .findFirst();
    if (phenotypeIndication() != null) {
      return phenoProp
          .map(studyProperty -> !phenotypeIndication().equals(studyProperty.getValue()))
          .orElse(true);
    }
    return false;
  }

  private boolean checkSpecies(Study study) {
    Optional<StudyProperty> speciesProp =
        study.getProperties().stream().filter(p -> p.getKey().equals(SPECIES_TYPE)).findFirst();
    if (species() != null) {
      return speciesProp
          .map(studyProperty -> !species().equals(studyProperty.getValue()))
          .orElse(true);
    }
    return false;
  }

  private boolean checkPiName(Study study) {
    return piName() != null && !piName().equals(study.getPiName()) && !piName().isBlank();
  }

  private boolean checkDataCustodians(Study study) {
    Optional<StudyProperty> custodiansProp =
        study.getProperties().stream()
            .filter(p -> p.getKey().equals(DATA_CUSTODIAN_EMAIL))
            .findFirst();
    if (dataCustodianEmail() != null) {
      if (custodiansProp.isEmpty()) {
        return true;
      }
      List<String> existingCustodians =
          GsonUtil.getInstance()
              .fromJson(
                  custodiansProp.get().getValue().toString(),
                  new TypeToken<ArrayList<String>>() {}.getType());
      return !CollectionUtils.isEqualCollection(dataCustodianEmail(), existingCustodians);
    }
    return false;
  }

  private boolean checkTargetDate(Study study) {
    Optional<StudyProperty> targetDateProp =
        study.getProperties().stream()
            .filter(p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_DELIVERY_DATE))
            .findFirst();
    if (alternativeDataSharingPlanTargetDeliveryDate() != null) {
      return targetDateProp
          .map(
              studyProperty ->
                  !alternativeDataSharingPlanTargetDeliveryDate().equals(studyProperty.getValue()))
          .orElse(true);
    }
    return false;
  }

  private boolean checkTargetReleaseDate(Study study) {
    Optional<StudyProperty> targetReleaseProp =
        study.getProperties().stream()
            .filter(p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_PUBLIC_RELEASE_DATE))
            .findFirst();
    if (alternativeDataSharingPlanTargetPublicReleaseDate() != null) {
      return targetReleaseProp
          .map(
              studyProperty ->
                  !alternativeDataSharingPlanTargetPublicReleaseDate()
                      .equals(studyProperty.getValue()))
          .orElse(true);
    }
    return false;
  }

  private boolean checkPublicVisibility(Study study) {
    return publicVisibility() != null && !publicVisibility().equals(study.getPublicVisibility());
  }
}
