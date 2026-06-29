package org.broadinstitute.consent.http.models.dto.registration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.BadRequestException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.NihAnvilUse;
import org.broadinstitute.consent.http.service.DatasetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudyUpdateRequestValidatorTest {

  @Mock private DatasetService datasetService;
  private StudyUpdateRequestValidator validator;

  @BeforeEach
  void setUp() {
    validator = new StudyUpdateRequestValidator(datasetService);
  }

  @Test
  void testValidate_valid() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    assertTrue(validator.validate(study, registration));
  }

  // ── Study name uniqueness ────────────────────────────────────────────────

  @Test
  void testValidate_studyName_null_skipsUniquenessCheck() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    registration.setStudyName(null);
    // null name — uniqueness check is skipped entirely, no service call needed
    assertDoesNotThrow(() -> validator.validate(study, registration));
  }

  @Test
  void testValidate_studyName_blank_rejected() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    registration.setStudyName("   ");
    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
    verify(datasetService, never()).findAllStudyNames();
  }

  @Test
  void testValidate_studyName_unchanged_doesNotQueryService() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    // name unchanged — service must not be called, so no stub needed
    assertDoesNotThrow(() -> validator.validate(study, registration));
    verify(datasetService, never()).findAllStudyNames();
  }

  @Test
  void testValidate_studyName_changedToAvailableName() {
    Study study = createMockStudy();
    when(datasetService.findAllStudyNames()).thenReturn(Set.of(study.getName()));
    StudyUpdateRequest registration = createValidRegistration(study);
    registration.setStudyName("A Brand New Name");
    assertDoesNotThrow(() -> validator.validate(study, registration));
  }

  @Test
  void testValidate_studyName_changedToExistingName() {
    String takenName = RandomStringUtils.secureStrong().nextAlphabetic(12);
    Study study = createMockStudy();
    when(datasetService.findAllStudyNames()).thenReturn(Set.of(study.getName(), takenName));
    StudyUpdateRequest registration = createValidRegistration(study);
    registration.setStudyName(takenName);
    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
  }

  // ── Required fields ──────────────────────────────────────────────────────

  @Test
  void testValidate_studyDescription_null() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    registration.setStudyDescription(null);
    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
  }

  @Test
  void testValidate_dataTypes_null() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    registration.setDataTypes(null);
    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
  }

  @Test
  void testValidate_dataTypes_empty() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    registration.setDataTypes(List.of());
    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
  }

  @Test
  void testValidate_publicVisibility_null() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    registration.setPublicVisibility(null);
    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
  }

  @Test
  void testValidate_nihAnvilUse_null() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    registration.setNihAnvilUse(null);
    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
  }

  @Test
  void testValidate_piName_null() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    registration.setPiName(null);
    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
  }

  // ── NIH conditional fields ───────────────────────────────────────────────

  @Test
  void testValidate_dbGaPPhsID_required() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    registration.setNihAnvilUse(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY);
    registration.setDbGaPPhsID(null);
    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
  }

  @Test
  void testValidate_piInstitution_required_for_dbgap() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    registration.setNihAnvilUse(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY);
    registration.setDbGaPPhsID(RandomStringUtils.secureStrong().nextAlphabetic(8));
    registration.setPiInstitution(null);
    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
  }

  @Test
  void testValidate_nihGrantContractNumber_required_for_dbgap() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    registration.setNihAnvilUse(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY);
    registration.setDbGaPPhsID(RandomStringUtils.secureStrong().nextAlphabetic(8));
    registration.setPiInstitution(RandomUtils.secureStrong().randomInt(1, 100));
    registration.setNihGrantContractNumber(null);
    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
  }

  @Test
  void testValidate_piInstitution_required_for_seeking_anvil() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    registration.setNihAnvilUse(
        NihAnvilUse.I_AM_NOT_NHGRI_FUNDED_BUT_I_AM_SEEKING_TO_SUBMIT_DATA_TO_AN_VIL);
    registration.setPiInstitution(null);
    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
  }

  @Test
  void testValidate_nihGrantContractNumber_required_for_seeking_anvil() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    registration.setNihAnvilUse(
        NihAnvilUse.I_AM_NOT_NHGRI_FUNDED_BUT_I_AM_SEEKING_TO_SUBMIT_DATA_TO_AN_VIL);
    registration.setPiInstitution(RandomUtils.secureStrong().randomInt(1, 100));
    registration.setNihGrantContractNumber(null);
    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
  }

  // ── Consent group membership ─────────────────────────────────────────────

  @Test
  void testValidate_consentGroupMembership_nonStudyDataset() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    // Force a datasetId that is not in the study (mock study uses IDs in 10–99)
    registration.getConsentGroups().get(0).setDatasetId(10000);
    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
  }

  @Test
  void testValidate_consentGroupMembership_studyDataset() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    // datasetId already matches the study dataset from createValidRegistration
    assertDoesNotThrow(() -> validator.validate(study, registration));
  }

  // ── Consent group name changes ───────────────────────────────────────────

  @Test
  void testValidate_consentGroupNameChange_blocked_whenSubmittedNameDiffers() {
    Study study = createMockStudy();
    study.getDatasets().forEach(d -> d.setName("Existing Name"));
    StudyUpdateRequest registration = createValidRegistration(study);
    // createValidRegistration uses a random name which differs from "Existing Name"
    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
  }

  @Test
  void testValidate_consentGroupNameChange_allowed_whenSubmittedNameMatchesStored() {
    Study study = createMockStudy();
    String storedName = "Existing Name";
    study.getDatasets().forEach(d -> d.setName(storedName));
    StudyUpdateRequest registration = createValidRegistration(study);
    // Round-trip: submitted name == stored name — should not be treated as a rename
    registration.getConsentGroups().forEach(cg -> cg.setConsentGroupName(storedName));
    assertDoesNotThrow(() -> validator.validate(study, registration));
  }

  @Test
  void testValidate_consentGroupNameChange_allowed_whenDatasetNotInDatasets() {
    // datasetId is in getDatasetIds() (membership passes) but getDatasets() is empty,
    // so dataset lookup returns empty — the return false; branch is exercised
    Study study = new Study();
    study.setName(RandomStringUtils.secureStrong().nextAlphabetic(10));
    int datasetId = RandomUtils.secureStrong().randomInt(10, 99);
    study.addDatasetIds(Set.of(datasetId));
    // intentionally no datasets added via addDatasets()

    StudyUpdateRequest registration = createValidRegistration(study);
    ConsentGroupRequest cg =
        registration.getConsentGroups().isEmpty()
            ? new ConsentGroupRequest()
            : registration.getConsentGroups().get(0);
    cg.setDatasetId(datasetId);
    cg.setConsentGroupName(RandomStringUtils.secureStrong().nextAlphabetic(10));
    if (registration.getConsentGroups().isEmpty()) {
      registration.setConsentGroups(new ArrayList<>(List.of(cg)));
    }
    assertDoesNotThrow(() -> validator.validate(study, registration));
  }

  @Test
  void testValidate_consentGroupNameChange_allowed_whenStoredNameIsNull() {
    Study study = createMockStudy();
    study.getDatasets().forEach(d -> d.setName(null));
    StudyUpdateRequest registration = createValidRegistration(study);
    assertDoesNotThrow(() -> validator.validate(study, registration));
  }

  @Test
  void testValidate_consentGroupNameChange_allowed_whenNameIsBlank() {
    Study study = createMockStudy();
    study.getDatasets().forEach(d -> d.setName(""));
    StudyUpdateRequest registration = createValidRegistration(study);
    assertDoesNotThrow(() -> validator.validate(study, registration));
  }

  // ── Consent group removal ────────────────────────────────────────────────

  @Test
  void testValidate_consentGroupRemoval_fails_whenDatasetOmitted() {
    Study study = createMockStudy();
    // Build the registration before adding the extra dataset so the payload omits it
    StudyUpdateRequest registration = createValidRegistration(study);

    Dataset extraDataset = new Dataset();
    extraDataset.setName("");
    extraDataset.setDatasetId(10000);
    extraDataset.setDacId(RandomUtils.secureStrong().randomInt(1, 100));
    study.getDatasets().add(extraDataset);
    List<Integer> ids = new ArrayList<>(study.getDatasetIds());
    ids.add(extraDataset.getDatasetId());
    study.addDatasetIds(new HashSet<>(ids));

    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
  }

  @Test
  void testValidate_consentGroupRemoval_emptyListIsNoOp() {
    // empty list is treated the same as null — "no consent group changes"
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    registration.setConsentGroups(List.of());
    assertDoesNotThrow(() -> validator.validate(study, registration));
  }

  // ── New consent group validation ─────────────────────────────────────────

  @Test
  void testValidate_newConsentGroup_valid() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    ConsentGroupRequest newCg = new ConsentGroupRequest();
    newCg.setConsentGroupName(RandomStringUtils.secureStrong().nextAlphabetic(10));
    newCg.setNumberOfParticipants(RandomUtils.secureStrong().randomInt(1, 100));
    newCg.setAccessManagement(AccessManagement.OPEN);
    // No datasetId — this is a new consent group
    List<ConsentGroupRequest> groups = new ArrayList<>(registration.getConsentGroups());
    groups.add(newCg);
    registration.setConsentGroups(groups);
    assertDoesNotThrow(() -> validator.validate(study, registration));
  }

  @Test
  void testValidate_newConsentGroup_missingName() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    ConsentGroupRequest newCg = new ConsentGroupRequest();
    newCg.setConsentGroupName(null);
    newCg.setNumberOfParticipants(RandomUtils.secureStrong().randomInt(1, 100));
    newCg.setAccessManagement(AccessManagement.OPEN);
    List<ConsentGroupRequest> groups = new ArrayList<>(registration.getConsentGroups());
    groups.add(newCg);
    registration.setConsentGroups(groups);
    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
  }

  @Test
  void testValidate_newConsentGroup_missingParticipants() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    ConsentGroupRequest newCg = new ConsentGroupRequest();
    newCg.setConsentGroupName(RandomStringUtils.secureStrong().nextAlphabetic(10));
    newCg.setNumberOfParticipants(null);
    newCg.setAccessManagement(AccessManagement.OPEN);
    List<ConsentGroupRequest> groups = new ArrayList<>(registration.getConsentGroups());
    groups.add(newCg);
    registration.setConsentGroups(groups);
    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
  }

  @Test
  void testValidate_newConsentGroup_noPrimaryDataUse() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    ConsentGroupRequest newCg = new ConsentGroupRequest();
    newCg.setConsentGroupName(RandomStringUtils.secureStrong().nextAlphabetic(10));
    newCg.setNumberOfParticipants(RandomUtils.secureStrong().randomInt(1, 100));
    // no accessManagement, no data-use flags
    List<ConsentGroupRequest> groups = new ArrayList<>(registration.getConsentGroups());
    groups.add(newCg);
    registration.setConsentGroups(groups);
    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
  }

  @Test
  void testValidate_newConsentGroup_controlled_dacRequired() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    ConsentGroupRequest newCg = new ConsentGroupRequest();
    newCg.setConsentGroupName(RandomStringUtils.secureStrong().nextAlphabetic(10));
    newCg.setNumberOfParticipants(RandomUtils.secureStrong().randomInt(1, 100));
    newCg.setAccessManagement(AccessManagement.CONTROLLED);
    newCg.setGeneralResearchUse(true);
    // dataAccessCommitteeId intentionally absent
    List<ConsentGroupRequest> groups = new ArrayList<>(registration.getConsentGroups());
    groups.add(newCg);
    registration.setConsentGroups(groups);
    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
  }

  @Test
  void testValidate_existingConsentGroup_dataUseNotValidated() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    // Existing consent group (has datasetId) with no data-use fields — should not be validated
    ConsentGroupRequest existingCg = registration.getConsentGroups().get(0);
    existingCg.setAccessManagement(null);
    existingCg.setGeneralResearchUse(null);
    // datasetId is already set (from createValidRegistration)
    assertDoesNotThrow(() -> validator.validate(study, registration));
  }

  // ── Conditional fields (update path) ────────────────────────────────────

  @Test
  void testValidate_gsrExplanation_required_when_gsr_true() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    registration.setControlledAccessRequiredForGenomicSummaryResultsGSR(true);
    registration.setControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation(null);
    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
  }

  @Test
  void testValidate_altSharingPlanExplanation_required() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    registration.setAlternativeDataSharingPlan(true);
    registration.setAlternativeDataSharingPlanExplanation(null);
    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
  }

  @Test
  void testValidate_altSharingPlanReasons_required() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    registration.setAlternativeDataSharingPlan(true);
    registration.setAlternativeDataSharingPlanExplanation(
        RandomStringUtils.secureStrong().nextAlphabetic(10));
    registration.setAlternativeDataSharingPlanReasons(List.of());
    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
  }

  // ── Email validation (update path) ──────────────────────────────────────

  @Test
  void testValidate_piEmail_invalid() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    registration.setPiEmail("not-an-email");
    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"   ", "pi@example.com"})
  void testValidate_piEmail_allowed(String email) {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    registration.setPiEmail(email);
    assertDoesNotThrow(() -> validator.validate(study, registration));
  }

  @Test
  void testValidate_dataCustodianEmail_invalid() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    registration.setDataCustodianEmail(List.of("valid@example.com", "not-an-email"));
    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
  }

  @Test
  void testValidate_dataCustodianEmail_valid() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    registration.setDataCustodianEmail(List.of("a@example.com", "b@example.org"));
    assertDoesNotThrow(() -> validator.validate(study, registration));
  }

  // ── Date validation (update path) ───────────────────────────────────────

  @Test
  void testValidate_embargoReleaseDate_invalid() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    registration.setEmbargoReleaseDate("01/15/2025");
    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
  }

  @Test
  void testValidate_embargoReleaseDate_valid() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    registration.setEmbargoReleaseDate("2025-01-15");
    assertDoesNotThrow(() -> validator.validate(study, registration));
  }

  @Test
  void testValidate_altSharingDeliveryDate_invalid() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    registration.setAlternativeDataSharingPlanTargetDeliveryDate("not-a-date");
    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
  }

  // ── morDate in new consent groups (update path) ──────────────────────────

  @Test
  void testValidate_newConsentGroup_morDate_invalid() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    ConsentGroupRequest newCg = new ConsentGroupRequest();
    newCg.setConsentGroupName(RandomStringUtils.secureStrong().nextAlphabetic(10));
    newCg.setNumberOfParticipants(RandomUtils.secureStrong().randomInt(1, 100));
    newCg.setAccessManagement(AccessManagement.OPEN);
    newCg.setMorDate("January 15, 2025");
    List<ConsentGroupRequest> groups = new ArrayList<>(registration.getConsentGroups());
    groups.add(newCg);
    registration.setConsentGroups(groups);
    assertThrows(BadRequestException.class, () -> validator.validate(study, registration));
  }

  @Test
  void testValidate_newConsentGroup_morDate_valid() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    ConsentGroupRequest newCg = new ConsentGroupRequest();
    newCg.setConsentGroupName(RandomStringUtils.secureStrong().nextAlphabetic(10));
    newCg.setNumberOfParticipants(RandomUtils.secureStrong().randomInt(1, 100));
    newCg.setAccessManagement(AccessManagement.OPEN);
    newCg.setMorDate("2025-06-01");
    List<ConsentGroupRequest> groups = new ArrayList<>(registration.getConsentGroups());
    groups.add(newCg);
    registration.setConsentGroups(groups);
    assertDoesNotThrow(() -> validator.validate(study, registration));
  }

  // ── Null consent groups ──────────────────────────────────────────────────

  @Test
  void testValidate_nullConsentGroups_passesAllGuards() {
    Study study = createMockStudy();
    StudyUpdateRequest registration = createValidRegistration(study);
    registration.setConsentGroups(null);
    // null consent groups skips membership, name-change, removal, and new-group checks
    assertDoesNotThrow(() -> validator.validate(study, registration));
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  private Study createMockStudy() {
    Study study = new Study();
    study.setName(RandomStringUtils.secureStrong().nextAlphabetic(10));
    Dataset dataset = new Dataset();
    dataset.setName("");
    dataset.setDatasetId(RandomUtils.secureStrong().randomInt(10, 99));
    dataset.setDacId(RandomUtils.secureStrong().randomInt(1, 100));
    study.addDatasets(List.of(dataset));
    study.addDatasetIds(Set.of(dataset.getDatasetId()));
    return study;
  }

  private StudyUpdateRequest createValidRegistration(Study study) {
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
                  // existing consent groups omit data-use fields — they were already validated
                  return cg;
                })
            .toList();
    registration.setConsentGroups(new ArrayList<>(consentGroups));
    return registration;
  }
}
