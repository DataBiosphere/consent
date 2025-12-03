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

  // Utility method to determine if any patch values differ from the provided Study entity
  public boolean isPatchable(Study study) {
    if (name() != null && !name().equals(study.getName()) && !name().isBlank()) {
      return true;
    }

    Optional<StudyProperty> studyTypeProp =
        study.getProperties().stream().filter(p -> p.getKey().equals("studyType")).findFirst();
    if (studyType() != null) {
      if (studyTypeProp.isEmpty()) {
        return true;
      }
      if (!studyType().value().equals(studyTypeProp.get().getValue())) {
        return true;
      }
    }

    if (description() != null
        && !description().equals(study.getDescription())
        && !description().isBlank()) {
      return true;
    }

    if (dataTypes() != null
        && !CollectionUtils.isEqualCollection(dataTypes(), study.getDataTypes())) {
      return true;
    }

    Optional<StudyProperty> phenoProp =
        study.getProperties().stream()
            .filter(p -> p.getKey().equals("phenotypeIndication"))
            .findFirst();
    if (phenotypeIndication() != null) {
      if (phenoProp.isEmpty()) {
        return true;
      }
      if (!phenotypeIndication().equals(phenoProp.get().getValue())) {
        return true;
      }
    }

    Optional<StudyProperty> speciesProp =
        study.getProperties().stream().filter(p -> p.getKey().equals("species")).findFirst();
    if (species() != null) {
      if (speciesProp.isEmpty()) {
        return true;
      }
      if (!species().equals(speciesProp.get().getValue())) {
        return true;
      }
    }

    if (piName() != null && !piName().equals(study.getPiName()) && !piName().isBlank()) {
      return true;
    }

    Optional<StudyProperty> custodiansProp =
        study.getProperties().stream()
            .filter(p -> p.getKey().equals("dataCustodianEmail"))
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
      if (!CollectionUtils.isEqualCollection(dataCustodianEmail(), existingCustodians)) {
        return true;
      }
    }

    Optional<StudyProperty> targetDateProp =
        study.getProperties().stream()
            .filter(p -> p.getKey().equals("alternativeDataSharingPlanTargetDeliveryDate"))
            .findFirst();
    if (alternativeDataSharingPlanTargetDeliveryDate() != null) {
      if (targetDateProp.isEmpty()) {
        return true;
      }
      if (!alternativeDataSharingPlanTargetDeliveryDate().equals(targetDateProp.get().getValue())) {
        return true;
      }
    }

    Optional<StudyProperty> targetReleaseProp =
        study.getProperties().stream()
            .filter(p -> p.getKey().equals("alternativeDataSharingPlanTargetPublicReleaseDate"))
            .findFirst();
    if (alternativeDataSharingPlanTargetPublicReleaseDate() != null) {
      if (targetReleaseProp.isEmpty()) {
        return true;
      }
      if (!alternativeDataSharingPlanTargetPublicReleaseDate()
          .equals(targetReleaseProp.get().getValue())) {
        return true;
      }
    }

    return publicVisibility() != null && !publicVisibility().equals(study.getPublicVisibility());
  }
}
