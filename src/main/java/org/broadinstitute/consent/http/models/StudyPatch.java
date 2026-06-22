package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    String piEmail,
    List<String> dataCustodianEmail,
    String alternativeDataSharingPlanTargetDeliveryDate,
    String alternativeDataSharingPlanTargetPublicReleaseDate,
    Boolean publicVisibility,
    String externalIdentifier,
    String externalIdentifierType) {

  public static final String STUDY_TYPE = "studyType";
  public static final String PHENOTYPE_INDICATION = "phenotypeIndication";
  // nosemgrep
  public static final String SPECIES_KEY = "species";
  public static final String DATA_CUSTODIAN_EMAIL = "dataCustodianEmail";
  public static final String ALTERNATIVE_DATA_SHARING_PLAN_TARGET_DELIVERY_DATE =
      "alternativeDataSharingPlanTargetDeliveryDate";
  public static final String ALTERNATIVE_DATA_SHARING_PLAN_TARGET_PUBLIC_RELEASE_DATE =
      "alternativeDataSharingPlanTargetPublicReleaseDate";
  public static final String EXTERNAL_IDENTIFIER = "externalIdentifier";
  public static final String EXTERNAL_IDENTIFIER_TYPE = "externalIdentifierType";

  public static StudyPatch fromJson(String json) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    mapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, true);
    SimpleModule module = new SimpleModule();
    module.addDeserializer(String.class, new ForceStringDeserializer());
    module.addDeserializer(Boolean.class, new ForceBooleanDeserializer());
    mapper.registerModule(module);
    try {
      return mapper.readValue(json, StudyPatch.class);
    } catch (Exception e) {
      throw new IllegalArgumentException(e.getMessage());
    }
  }

  // Jackson, by default, allows coercion from numbers to strings.
  // This custom deserializer forbids that behavior for all String fields.
  private static class ForceStringDeserializer extends JsonDeserializer<String> {
    @Override
    public String deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
        throws IOException {
      if (jsonParser.getCurrentToken() == JsonToken.VALUE_NUMBER_INT) {
        throw deserializationContext.wrongTokenException(
            jsonParser,
            String.class,
            JsonToken.VALUE_STRING,
            "Attempted to parse int to string but this is not allowed");
      }
      return jsonParser.getValueAsString();
    }
  }

  // Jackson, by default, allows coercion from strings to booleans.
  // This custom deserializer forbids that behavior for all Boolean fields.
  private static class ForceBooleanDeserializer extends JsonDeserializer<Boolean> {
    @Override
    public Boolean deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
        throws IOException {
      if (jsonParser.getCurrentToken() == JsonToken.VALUE_STRING) {
        throw deserializationContext.wrongTokenException(
            jsonParser,
            String.class,
            JsonToken.NOT_AVAILABLE,
            "Attempted to parse string to boolean but this is not allowed");
      }
      return jsonParser.getBooleanValue();
    }
  }

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
    checks.add(checkPiEmail(study));
    checks.add(checkDataCustodians(study));
    checks.add(checkTargetDate(study));
    checks.add(checkTargetReleaseDate(study));
    checks.add(checkPublicVisibility(study));
    checks.add(checkExternalIdentifier(study));
    checks.add(checkExternalIdentifierType(study));
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
    return checkStringProperty(study, PHENOTYPE_INDICATION, phenotypeIndication());
  }

  private boolean checkSpecies(Study study) {
    return checkStringProperty(study, SPECIES_KEY, species());
  }

  private boolean checkPiName(Study study) {
    return piName() != null && !piName().equals(study.getPiName()) && !piName().isBlank();
  }

  private boolean checkPiEmail(Study study) {
    return piEmail() != null && !piEmail().equals(study.getPiEmail()) && !piEmail().isBlank();
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
    return checkStringProperty(
        study,
        ALTERNATIVE_DATA_SHARING_PLAN_TARGET_DELIVERY_DATE,
        alternativeDataSharingPlanTargetDeliveryDate());
  }

  private boolean checkTargetReleaseDate(Study study) {
    return checkStringProperty(
        study,
        ALTERNATIVE_DATA_SHARING_PLAN_TARGET_PUBLIC_RELEASE_DATE,
        alternativeDataSharingPlanTargetPublicReleaseDate());
  }

  private boolean checkPublicVisibility(Study study) {
    return publicVisibility() != null && !publicVisibility().equals(study.getPublicVisibility());
  }

  private boolean checkExternalIdentifier(Study study) {
    return checkStringProperty(study, EXTERNAL_IDENTIFIER, externalIdentifier());
  }

  private boolean checkExternalIdentifierType(Study study) {
    return checkStringProperty(study, EXTERNAL_IDENTIFIER_TYPE, externalIdentifierType());
  }

  // null=no-op, blank=delete (patchable if property exists), non-blank=upsert
  private boolean checkStringProperty(Study study, String key, String patchValue) {
    if (patchValue == null) return false;
    Optional<StudyProperty> prop =
        study.getProperties().stream().filter(p -> p.getKey().equals(key)).findFirst();
    if (patchValue.isBlank()) return prop.isPresent();
    return prop.map(p -> !patchValue.equals(p.getValue())).orElse(true);
  }

  // Returns all optional string-typed study properties from this patch (null values included).
  // null=no-op, blank=delete, non-blank=upsert
  public Map<String, String> stringPatchProps() {
    Map<String, String> props = new HashMap<>();
    props.put(PHENOTYPE_INDICATION, phenotypeIndication());
    props.put(SPECIES_KEY, species());
    props.put(
        ALTERNATIVE_DATA_SHARING_PLAN_TARGET_DELIVERY_DATE,
        alternativeDataSharingPlanTargetDeliveryDate());
    props.put(
        ALTERNATIVE_DATA_SHARING_PLAN_TARGET_PUBLIC_RELEASE_DATE,
        alternativeDataSharingPlanTargetPublicReleaseDate());
    props.put(EXTERNAL_IDENTIFIER, externalIdentifier());
    props.put(EXTERNAL_IDENTIFIER_TYPE, externalIdentifierType());
    return props;
  }
}
