package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
    Integer piInstitutionId,
    String piOrcid,
    String piLinkedinUrl,
    String piWebsiteUrl,
    List<String> dataCustodianEmail,
    String alternativeDataSharingPlanTargetDeliveryDate,
    String alternativeDataSharingPlanTargetPublicReleaseDate,
    Boolean publicVisibility,
    String externalIdentifier,
    String externalIdentifierType,
    // Field names the client sent as an explicit JSON null. Jackson cannot tell an absent field
    // from an explicit null, so fromJson records them here; see the PI column convention below.
    @JsonIgnore Set<String> explicitNulls) {

  /**
   * Convenience constructor for callers that build a patch directly rather than from a request
   * body. Such a patch has no wire representation, so no field was explicitly nulled.
   */
  public StudyPatch(
      String name,
      StudyType studyType,
      String description,
      List<String> dataTypes,
      String phenotypeIndication,
      String species,
      String piName,
      String piEmail,
      Integer piInstitutionId,
      String piOrcid,
      String piLinkedinUrl,
      String piWebsiteUrl,
      List<String> dataCustodianEmail,
      String alternativeDataSharingPlanTargetDeliveryDate,
      String alternativeDataSharingPlanTargetPublicReleaseDate,
      Boolean publicVisibility,
      String externalIdentifier,
      String externalIdentifierType) {
    this(
        name,
        studyType,
        description,
        dataTypes,
        phenotypeIndication,
        species,
        piName,
        piEmail,
        piInstitutionId,
        piOrcid,
        piLinkedinUrl,
        piWebsiteUrl,
        dataCustodianEmail,
        alternativeDataSharingPlanTargetDeliveryDate,
        alternativeDataSharingPlanTargetPublicReleaseDate,
        publicVisibility,
        externalIdentifier,
        externalIdentifierType,
        Set.of());
  }

  public StudyPatch {
    explicitNulls = explicitNulls == null ? Set.of() : Set.copyOf(explicitNulls);
  }

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

  /**
   * The PI detail columns. Unlike the patchable study properties — where a blank string signals a
   * delete — these are columns on the study row, so they follow the JSON convention: an absent
   * field is a no-op, an explicit {@code null} clears the column, and a value sets it. A blank
   * string is normalized to a clear rather than stored, since an empty ORCID or URL is never
   * meaningful.
   */
  public static final String PI_INSTITUTION_ID = "piInstitutionId";

  public static final String PI_ORCID = "piOrcid";
  public static final String PI_LINKEDIN_URL = "piLinkedinUrl";
  public static final String PI_WEBSITE_URL = "piWebsiteUrl";

  public static StudyPatch fromJson(String json) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    mapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, true);
    SimpleModule module = new SimpleModule();
    module.addDeserializer(String.class, new ForceStringDeserializer());
    module.addDeserializer(Boolean.class, new ForceBooleanDeserializer());
    mapper.registerModule(module);
    try {
      StudyPatch patch = mapper.readValue(json, StudyPatch.class);
      return patch.withExplicitNulls(explicitNullFields(mapper, json));
    } catch (Exception e) {
      throw new IllegalArgumentException(e.getMessage());
    }
  }

  /**
   * The names of the fields the body set to a literal JSON null. Binding alone cannot report these,
   * because Jackson leaves both an absent field and an explicit null as a null component.
   */
  private static Set<String> explicitNullFields(ObjectMapper mapper, String json)
      throws IOException {
    JsonNode root = mapper.readTree(json);
    if (root == null || !root.isObject()) {
      return Set.of();
    }
    return root.properties().stream()
        .filter(entry -> entry.getValue().isNull())
        .map(Map.Entry::getKey)
        .collect(Collectors.toUnmodifiableSet());
  }

  private StudyPatch withExplicitNulls(Set<String> fields) {
    return new StudyPatch(
        name,
        studyType,
        description,
        dataTypes,
        phenotypeIndication,
        species,
        piName,
        piEmail,
        piInstitutionId,
        piOrcid,
        piLinkedinUrl,
        piWebsiteUrl,
        dataCustodianEmail,
        alternativeDataSharingPlanTargetDeliveryDate,
        alternativeDataSharingPlanTargetPublicReleaseDate,
        publicVisibility,
        externalIdentifier,
        externalIdentifierType,
        fields);
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
    checks.add(checkPiInstitutionId(study));
    checks.add(checkNullableColumn(PI_ORCID, piOrcid(), study.getPiOrcid()));
    checks.add(checkNullableColumn(PI_LINKEDIN_URL, piLinkedinUrl(), study.getPiLinkedinUrl()));
    checks.add(checkNullableColumn(PI_WEBSITE_URL, piWebsiteUrl(), study.getPiWebsiteUrl()));
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

  private boolean checkPiInstitutionId(Study study) {
    Integer existing = study.getPiInstitution() == null ? null : study.getPiInstitution().getId();
    return !Objects.equals(resolvePiInstitutionId(existing), existing);
  }

  private boolean checkNullableColumn(String field, String patchValue, String existing) {
    return !Objects.equals(resolveColumn(field, patchValue, existing), existing);
  }

  /**
   * Resolves the PI institution id against its stored value: absent=keep existing, explicit
   * null=clear, a value=set.
   */
  public Integer resolvePiInstitutionId(Integer existing) {
    if (piInstitutionId() != null) {
      return piInstitutionId();
    }
    return explicitNulls().contains(PI_INSTITUTION_ID) ? null : existing;
  }

  public String resolvePiOrcid(String existing) {
    return resolveColumn(PI_ORCID, piOrcid(), existing);
  }

  public String resolvePiLinkedinUrl(String existing) {
    return resolveColumn(PI_LINKEDIN_URL, piLinkedinUrl(), existing);
  }

  public String resolvePiWebsiteUrl(String existing) {
    return resolveColumn(PI_WEBSITE_URL, piWebsiteUrl(), existing);
  }

  /**
   * Resolves a nullable string column against its stored value: absent=keep existing, explicit
   * null=clear, blank=clear (an empty value is never stored), any other value=set.
   */
  private String resolveColumn(String field, String patchValue, String existing) {
    if (patchValue == null) {
      return explicitNulls().contains(field) ? null : existing;
    }
    return patchValue.isBlank() ? null : patchValue;
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
