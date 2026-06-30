package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.NihAnvilUse;
import org.broadinstitute.consent.http.models.dto.registration.ConsentGroupRequest;
import org.broadinstitute.consent.http.models.dto.registration.StudyRegistrationRequest;
import org.broadinstitute.consent.http.models.dto.registration.StudyUpdateRequest;
import org.broadinstitute.consent.http.service.RegistrationShadowValidator.ComparisonResult;
import org.broadinstitute.consent.http.service.RegistrationShadowValidator.ValidationOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrationShadowValidatorTest {

  @Mock private DatasetService datasetService;

  private final ObjectMapper mapper = new ObjectMapper();
  private RegistrationShadowValidator shadowValidator;

  @BeforeEach
  void setUp() {
    shadowValidator = new RegistrationShadowValidator(datasetService);
  }

  // ── ValidationOutcome ─────────────────────────────────────────────────────

  @Test
  void testValidationOutcome_accepted() {
    ValidationOutcome outcome = ValidationOutcome.accepted(42L);
    assertTrue(outcome.accepted());
    assertEquals("", outcome.message());
    assertEquals(42L, outcome.durationNanos());
  }

  @Test
  void testValidationOutcome_rejected() {
    ValidationOutcome outcome = ValidationOutcome.rejected("nope", 7L);
    assertFalse(outcome.accepted());
    assertEquals("nope", outcome.message());
    assertEquals(7L, outcome.durationNanos());
  }

  @Test
  void testValidationOutcome_acceptedSince() {
    long start = System.nanoTime();
    ValidationOutcome outcome = ValidationOutcome.acceptedSince(start);
    assertTrue(outcome.accepted());
    assertEquals("", outcome.message());
    assertTrue(outcome.durationNanos() >= 0);
  }

  @Test
  void testValidationOutcome_rejectedSince() {
    long start = System.nanoTime();
    ValidationOutcome outcome = ValidationOutcome.rejectedSince("nope", start);
    assertFalse(outcome.accepted());
    assertEquals("nope", outcome.message());
    assertTrue(outcome.durationNanos() >= 0);
  }

  // ── compareCreate ─────────────────────────────────────────────────────────

  @Test
  void testCompareCreate_agree_bothAccept() throws Exception {
    String json = mapper.writeValueAsString(createValidRegistration());
    ComparisonResult result = shadowValidator.compareCreate(json, ValidationOutcome.accepted(100L));
    assertTrue(result.agree());
    assertTrue(result.newOutcome().accepted());
  }

  @Test
  void testCompareCreate_disagree_newRejectsViaBadRequest() throws Exception {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setStudyName(null);
    String json = mapper.writeValueAsString(registration);

    ComparisonResult result = shadowValidator.compareCreate(json, ValidationOutcome.accepted(100L));
    assertFalse(result.agree());
    assertFalse(result.newOutcome().accepted());
    assertTrue(result.newOutcome().message().contains("Study Name is required"));
  }

  @Test
  void testCompareCreate_disagree_oldRejectsNewAccepts() throws Exception {
    String json = mapper.writeValueAsString(createValidRegistration());

    ComparisonResult result =
        shadowValidator.compareCreate(json, ValidationOutcome.rejected("old said no", 100L));
    assertFalse(result.agree());
    assertTrue(result.newOutcome().accepted());
  }

  @Test
  void testCompareCreate_malformedJson_rejectedWithDeserializationMessage() {
    ComparisonResult result =
        shadowValidator.compareCreate("not valid json", ValidationOutcome.rejected("bad", 1L));
    assertTrue(result.agree());
    assertFalse(result.newOutcome().accepted());
    assertTrue(result.newOutcome().message().startsWith("Unable to deserialize/validate:"));
  }

  @Test
  void testCompareCreate_nullOldOutcome_skipsComparison() throws Exception {
    String json = mapper.writeValueAsString(createValidRegistration());
    ComparisonResult result = shadowValidator.compareCreate(json, null);
    assertFalse(result.agree());
    assertNull(result.newOutcome());
    assertNull(result.oldOutcome());
  }

  // ── compareUpdate ─────────────────────────────────────────────────────────

  @Test
  void testCompareUpdate_agree_bothAccept() throws Exception {
    Study study = createMockStudy();
    String json = mapper.writeValueAsString(createValidUpdateRequest(study));

    ComparisonResult result =
        shadowValidator.compareUpdate(json, study, ValidationOutcome.accepted(100L));
    assertTrue(result.agree());
    assertTrue(result.newOutcome().accepted());
  }

  @Test
  void testCompareUpdate_disagree_newRejectsViaBadRequest() throws Exception {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidUpdateRequest(study);
    registration.setStudyDescription(null);
    String json = mapper.writeValueAsString(registration);

    ComparisonResult result =
        shadowValidator.compareUpdate(json, study, ValidationOutcome.accepted(100L));
    assertFalse(result.agree());
    assertFalse(result.newOutcome().accepted());
    assertTrue(result.newOutcome().message().contains("Study Description is required"));
  }

  @Test
  void testCompareUpdate_disagree_oldRejectsNewAccepts() throws Exception {
    Study study = createMockStudy();
    String json = mapper.writeValueAsString(createValidUpdateRequest(study));

    ComparisonResult result =
        shadowValidator.compareUpdate(json, study, ValidationOutcome.rejected("old said no", 100L));
    assertFalse(result.agree());
    assertTrue(result.newOutcome().accepted());
  }

  @Test
  void testCompareUpdate_malformedJson_rejectedWithDeserializationMessage() {
    Study study = createMockStudy();
    ComparisonResult result =
        shadowValidator.compareUpdate(
            "not valid json", study, ValidationOutcome.rejected("bad", 1L));
    assertTrue(result.agree());
    assertFalse(result.newOutcome().accepted());
    assertTrue(result.newOutcome().message().startsWith("Unable to deserialize/validate:"));
  }

  @Test
  void testCompareUpdate_nullOldOutcome_skipsComparison() throws Exception {
    Study study = createMockStudy();
    String json = mapper.writeValueAsString(createValidUpdateRequest(study));
    ComparisonResult result = shadowValidator.compareUpdate(json, study, null);
    assertFalse(result.agree());
    assertNull(result.newOutcome());
    assertNull(result.oldOutcome());
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  private ConsentGroupRequest createValidConsentGroup() {
    ConsentGroupRequest cg = new ConsentGroupRequest();
    cg.setConsentGroupName(RandomStringUtils.secureStrong().nextAlphabetic(10));
    cg.setNumberOfParticipants(RandomUtils.secureStrong().randomInt(1, 100));
    cg.setAccessManagement(AccessManagement.OPEN);
    return cg;
  }

  private StudyRegistrationRequest createValidRegistration() {
    StudyRegistrationRequest registration = new StudyRegistrationRequest();
    registration.setStudyName(RandomStringUtils.secureStrong().nextAlphabetic(10));
    registration.setStudyDescription(RandomStringUtils.secureStrong().nextAlphabetic(20));
    registration.setDataTypes(List.of(RandomStringUtils.secureStrong().nextAlphabetic(8)));
    registration.setPublicVisibility(true);
    registration.setNihAnvilUse(
        NihAnvilUse.I_AM_NOT_NHGRI_FUNDED_AND_DO_NOT_PLAN_TO_STORE_DATA_IN_AN_VIL);
    registration.setPiName(RandomStringUtils.secureStrong().nextAlphabetic(10));
    registration.setConsentGroups(List.of(createValidConsentGroup()));
    return registration;
  }

  private Study createMockStudy() {
    Study study = new Study();
    study.setName(RandomStringUtils.secureStrong().nextAlphabetic(10));
    Dataset dataset = new Dataset();
    dataset.setName("");
    dataset.setDatasetId(RandomUtils.secureStrong().randomInt(10, 99));
    dataset.setDacId(RandomUtils.secureStrong().randomInt(1, 100));
    study.addDatasets(List.of(dataset));
    study.addDatasetIds(java.util.Set.of(dataset.getDatasetId()));
    return study;
  }

  private StudyUpdateRequest createValidUpdateRequest(Study study) {
    StudyUpdateRequest registration = new StudyUpdateRequest();
    registration.setStudyName(study.getName());
    registration.setStudyDescription(RandomStringUtils.secureStrong().nextAlphabetic(20));
    registration.setDataTypes(List.of(RandomStringUtils.secureStrong().nextAlphabetic(8)));
    registration.setPublicVisibility(true);
    registration.setNihAnvilUse(
        NihAnvilUse.I_AM_NOT_NHGRI_FUNDED_AND_DO_NOT_PLAN_TO_STORE_DATA_IN_AN_VIL);
    registration.setPiName(RandomStringUtils.secureStrong().nextAlphabetic(10));

    List<ConsentGroupRequest> consentGroups =
        study.getDatasets().stream()
            .map(
                d -> {
                  ConsentGroupRequest cg = new ConsentGroupRequest();
                  cg.setConsentGroupName(RandomStringUtils.secureStrong().nextAlphabetic(10));
                  cg.setNumberOfParticipants(RandomUtils.secureStrong().randomInt(1, 100));
                  cg.setDatasetId(d.getDatasetId());
                  return cg;
                })
            .toList();
    registration.setConsentGroups(consentGroups);
    return registration;
  }
}
