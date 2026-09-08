package org.broadinstitute.consent.http.models.dto.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.reflect.TypeToken;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.broadinstitute.consent.http.models.dto.registration.RegistrationRequestMapper.ConsentGroupContext;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.Test;

class RegistrationRequestMapperTest extends AbstractTestHelper {

  private final RegistrationRequestMapper mapper = new RegistrationRequestMapper();

  @Test
  void testGenerateDataUseFromConsentGroup() {
    ConsentGroupRequest consentGroup = new ConsentGroupRequest();
    consentGroup.setGeneralResearchUse(false);
    consentGroup.setHmb(true);
    consentGroup.setDiseaseSpecificUse(List.of("disease1", "disease2"));
    consentGroup.setPoa(true);
    consentGroup.setNmds(true);
    consentGroup.setNpu(true);
    consentGroup.setOtherPrimary("other primary use");
    consentGroup.setOtherSecondary("other secondary use");
    consentGroup.setIrb(true);
    consentGroup.setCol(true);
    consentGroup.setGs("USA");
    consentGroup.setGso(true);
    consentGroup.setPub(true);
    consentGroup.setMor(true);
    consentGroup.setMorDate("2025-12-31");
    DataUse dataUse = mapper.toDataUse(consentGroup);
    assertDataUse(consentGroup, dataUse);
  }

  @Test
  void testExtractStudyProperty() {
    RegistrationRequestMapper.StudyPropertyExtractor extractor =
        new RegistrationRequestMapper.StudyPropertyExtractor(
            randomAlphabetic(10), PropertyType.String, StudyRegistrationRequest::getStudyName);

    StudyRegistrationRequest request = new StudyRegistrationRequest();

    // null value -> empty extraction
    assertTrue(extractor.extract(request).isEmpty());

    request.setStudyName(randomAlphabetic(10));

    Optional<StudyProperty> prop = extractor.extract(request);

    // non-null value -> turn value into dataset prop
    assertTrue(prop.isPresent());

    assertEquals(request.getStudyName(), prop.get().getValue());
    assertEquals(extractor.key(), prop.get().getKey());
    assertEquals(extractor.type(), prop.get().getType());
  }

  @Test
  void testExtractDatasetProperty() {
    RegistrationRequestMapper.DatasetPropertyExtractor extractor =
        RegistrationRequestMapper.DatasetPropertyExtractor.of(
            randomAlphabetic(10),
            randomAlphabetic(10),
            PropertyType.String,
            ConsentGroupRequest::getConsentGroupName);

    ConsentGroupRequest group = new ConsentGroupRequest();

    // null value -> empty extraction
    assertTrue(extractor.extract(group, ConsentGroupContext.NEW).isEmpty());

    group.setConsentGroupName(randomAlphabetic(10));

    Optional<DatasetProperty> prop = extractor.extract(group, ConsentGroupContext.NEW);

    // non-null value -> turn value into dataset prop
    assertTrue(prop.isPresent());

    assertEquals(group.getConsentGroupName(), prop.get().getPropertyValue());
    assertEquals(extractor.name(), prop.get().getPropertyName());
    assertEquals(extractor.schemaProp(), prop.get().getSchemaProperty());
    assertEquals(extractor.type(), prop.get().getPropertyType());
  }

  @Test
  void testExtractStudyPropertyTyped() {
    RegistrationRequestMapper.StudyPropertyExtractor extractor =
        new RegistrationRequestMapper.StudyPropertyExtractor(
            randomAlphabetic(10),
            PropertyType.Json,
            request -> GsonUtil.getInstance().toJson(request.getDataTypes()));

    StudyRegistrationRequest request = new StudyRegistrationRequest();

    request.setDataTypes(List.of("type1", "type2", "type3"));

    Optional<StudyProperty> prop = extractor.extract(request);

    assertTrue(prop.isPresent());

    assertEquals(GsonUtil.getInstance().toJsonTree(request.getDataTypes()), prop.get().getValue());
    assertEquals(extractor.key(), prop.get().getKey());
    assertEquals(extractor.type(), prop.get().getType());
  }

  @Test
  void testExtractDatasetPropertyTyped() {
    RegistrationRequestMapper.DatasetPropertyExtractor extractor =
        RegistrationRequestMapper.DatasetPropertyExtractor.of(
            randomAlphabetic(10),
            randomAlphabetic(10),
            PropertyType.Json,
            consentGroup -> GsonUtil.getInstance().toJson(consentGroup.getDiseaseSpecificUse()));

    ConsentGroupRequest group = new ConsentGroupRequest();

    group.setDiseaseSpecificUse(List.of("asdf", "sdfg", "dfgh"));

    Optional<DatasetProperty> prop = extractor.extract(group, ConsentGroupContext.NEW);

    assertTrue(prop.isPresent());

    assertEquals(
        GsonUtil.getInstance().toJsonTree(group.getDiseaseSpecificUse()),
        prop.get().getPropertyValue());
    assertEquals(extractor.name(), prop.get().getPropertyName());
    assertEquals(extractor.schemaProp(), prop.get().getSchemaProperty());
    assertEquals(extractor.type(), prop.get().getPropertyType());
  }

  @Test
  void testAccessManagementExcludedForExistingConsentGroupUpdate() {
    ConsentGroupRequest group = new ConsentGroupRequest();
    group.setDatasetId(42);
    group.setAccessManagement(AccessManagement.OPEN);

    List<DatasetProperty> props =
        mapper.toDatasetProperties(group, ConsentGroupContext.EXISTING_UPDATE);

    assertTrue(props.stream().noneMatch(p -> p.getSchemaProperty().equals("accessManagement")));
  }

  @Test
  void testAccessManagementIncludedForNewConsentGroup() {
    ConsentGroupRequest group = new ConsentGroupRequest();
    group.setAccessManagement(AccessManagement.OPEN);

    List<DatasetProperty> props = mapper.toDatasetProperties(group, ConsentGroupContext.NEW);

    Optional<DatasetProperty> accessManagementProp =
        props.stream().filter(p -> p.getSchemaProperty().equals("accessManagement")).findFirst();
    assertTrue(accessManagementProp.isPresent());
    assertEquals(AccessManagement.OPEN.value(), accessManagementProp.get().getPropertyValue());
  }

  @Test
  void testDataFieldNotExcludedForExistingConsentGroupUpdate() {
    ConsentGroupRequest group = new ConsentGroupRequest();
    group.setDatasetId(42);
    group.setData(Map.of("key", "value"));

    List<DatasetProperty> props =
        mapper.toDatasetProperties(group, ConsentGroupContext.EXISTING_UPDATE);

    Optional<DatasetProperty> dataProp =
        props.stream().filter(p -> p.getSchemaProperty().equals("data")).findFirst();
    assertTrue(dataProp.isPresent());
  }

  @Test
  void testStudyAssetsAndDataRoundTripNormalizeNumericValues() {
    // Map.of("count", 5) holds a plain java.lang.Integer, matching what Jackson produces
    // for a JSON integer field (unlike the legacy Gson-deserialized DatasetRegistrationSchemaV1
    // path). Confirm the mapper's Gson re-serialization of that value still round-trips
    // through GsonUtil's configured LONG_OR_DOUBLE number strategy without value loss.
    StudyRegistrationRequest request = new StudyRegistrationRequest();
    request.setAssets(Map.of("count", 5));

    List<StudyProperty> props = mapper.toStudyProperties(request);
    Optional<StudyProperty> assetsProp =
        props.stream().filter(p -> p.getKey().equals("assets")).findFirst();
    assertTrue(assetsProp.isPresent());

    Map<String, Object> roundTripped =
        GsonUtil.getInstance()
            .fromJson(
                assetsProp.get().getValue().toString(),
                new TypeToken<Map<String, Object>>() {}.getType());
    assertEquals(5L, roundTripped.get("count"));
  }

  /** A promoted asset list sent as a top-level field is stored in its own study property. */
  @Test
  void testPromotedAssetListsBecomeTheirOwnStudyProperties() {
    StudyRegistrationRequest request = new StudyRegistrationRequest();
    request.setModels(List.of(Map.of("modelId", "m-1")));
    request.setFunding(List.of(Map.of("grant", "R01")));

    List<StudyProperty> props = mapper.toStudyProperties(request);

    assertTrue(findProp(props, "models").isPresent());
    assertTrue(findProp(props, "funding").isPresent());
    // Nothing unpromoted was sent, so no legacy assets property is stored
    assertTrue(findProp(props, "assets").isEmpty());
  }

  /**
   * Until clients move to the top-level fields, a promoted list sent inside the deprecated assets
   * object is still stored as its promoted property, and stripped from the object.
   */
  @Test
  void testPromotedAssetListsSentInsideTheLegacyObjectAreStillPromoted() {
    StudyRegistrationRequest request = new StudyRegistrationRequest();
    request.setAssets(
        Map.of(
            "models", List.of(Map.of("modelId", "m-1")),
            "uiLabels", Map.of("tab", "Assets")));

    List<StudyProperty> props = mapper.toStudyProperties(request);

    assertTrue(findProp(props, "models").isPresent());
    Map<String, Object> remaining =
        GsonUtil.getInstance()
            .fromJson(
                findProp(props, "assets").orElseThrow().getValue().toString(),
                new TypeToken<Map<String, Object>>() {}.getType());
    assertEquals(Set.of("uiLabels"), remaining.keySet());
  }

  /** A top-level field wins over the same key inside the deprecated object. */
  @Test
  void testTopLevelPromotedFieldWinsOverTheLegacyObject() {
    StudyRegistrationRequest request = new StudyRegistrationRequest();
    request.setModels(List.of(Map.of("modelId", "top-level")));
    request.setAssets(Map.of("models", List.of(Map.of("modelId", "legacy"))));

    List<StudyProperty> props = mapper.toStudyProperties(request);

    assertTrue(findProp(props, "models").orElseThrow().getValue().toString().contains("top-level"));
    assertTrue(findProp(props, "assets").isEmpty());
  }

  /**
   * Registration reads return every promoted list both top-level and inside the deprecated assets
   * object, so an edit that removes the last entry arrives as an empty top-level list next to the
   * pre-edit legacy copy. The empty list is the submitter's intent and has to win, or the removed
   * entry comes straight back on save.
   */
  @Test
  void testClearedTopLevelPromotedFieldIsNotRestoredFromTheLegacyObject() {
    StudyRegistrationRequest request = new StudyRegistrationRequest();
    request.setPublications(List.of());
    request.setAssets(Map.of("publications", List.of(Map.of("title", "removed"))));

    List<StudyProperty> props = mapper.toStudyProperties(request);

    assertEquals("[]", findProp(props, "publications").orElseThrow().getValue().toString());
    assertTrue(findProp(props, "assets").isEmpty());
  }

  private Optional<StudyProperty> findProp(List<StudyProperty> props, String key) {
    return props.stream().filter(p -> p.getKey().equals(key)).findFirst();
  }

  private void assertDataUse(ConsentGroupRequest consentGroup, DataUse dataUse) {
    assertEquals(consentGroup.getCol(), dataUse.getCollaboratorRequired());
    assertEquals(consentGroup.getDiseaseSpecificUse(), dataUse.getDiseaseRestrictions());
    assertEquals(consentGroup.getIrb(), dataUse.getEthicsApprovalRequired());
    assertEquals(consentGroup.getGeneralResearchUse(), dataUse.getGeneralUse());
    assertEquals(consentGroup.getGs(), dataUse.getGeographicalRestrictions());
    assertEquals(consentGroup.getGso(), dataUse.getGeneticStudiesOnly());
    assertEquals(consentGroup.getHmb(), dataUse.getHmbResearch());
    assertEquals(consentGroup.getMorDate(), dataUse.getPublicationMoratorium());
    // NMDS is an inverse condition flag:
    // Methods research (analytic/software/technology development) is prohibited
    // https://github.com/EBISPOT/DUO
    if (Objects.isNull(consentGroup.getNmds()) || !consentGroup.getNmds()) {
      assertNull(dataUse.getMethodsResearch());
    } else {
      assertFalse(dataUse.getMethodsResearch());
    }
    assertEquals(consentGroup.getNpu(), dataUse.getNonProfitUse());
    assertEquals(consentGroup.getOtherPrimary(), dataUse.getOther());
    assertEquals(consentGroup.getOtherSecondary(), dataUse.getSecondaryOther());
    assertEquals(consentGroup.getPoa(), dataUse.getPopulationOriginsAncestry());
    assertEquals(consentGroup.getPub(), dataUse.getPublicationResults());
  }
}
