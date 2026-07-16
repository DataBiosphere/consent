package org.broadinstitute.consent.http.models.dto.registration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.BadRequestException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.dataset_registration_v1.AlternativeDataSharingPlanReason;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.NihAnvilUse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class StudyRegistrationRequestValidatorTest {

  private StudyRegistrationRequestValidator validator;

  @BeforeEach
  void setUp() {
    validator = new StudyRegistrationRequestValidator();
  }

  @Test
  void testCollectViolations_valid() {
    StudyRegistrationRequest registration = createValidRegistration();
    assertTrue(validator.collectViolations(registration).isEmpty());
  }

  // ── Required fields & invalid values ─────────────────────────────────────

  @ParameterizedTest
  @MethodSource({"invalidRegistrationMutations", "invalidDateMutations"})
  void testCollectViolations_invalidField_reportsViolation(
      Consumer<StudyRegistrationRequest> mutate) {
    StudyRegistrationRequest registration = createValidRegistration();
    mutate.accept(registration);
    assertFalse(validator.collectViolations(registration).isEmpty());
  }

  static Stream<Consumer<StudyRegistrationRequest>> invalidRegistrationMutations() {
    return Stream.of(
        r -> r.setStudyName(null),
        r -> r.setStudyName("  "),
        r -> r.setStudyDescription(null),
        r -> r.setDataTypes(null),
        r -> r.setDataTypes(List.of()),
        r -> r.setPublicVisibility(null),
        r -> r.setNihAnvilUse(null),
        r -> r.setPiName(null),
        r -> r.setConsentGroups(null),
        r -> r.setConsentGroups(List.of()),
        r -> r.getConsentGroups().getFirst().setConsentGroupName(null),
        r -> r.getConsentGroups().getFirst().setConsentGroupName("   "),
        r -> r.getConsentGroups().getFirst().setNumberOfParticipants(null));
  }

  // ── Data-use consistency ─────────────────────────────────────────────────

  @Test
  void testCollectViolations_dataUse_noPrimaryUse() {
    StudyRegistrationRequest registration = createValidRegistration();
    ConsentGroupRequest cg = registration.getConsentGroups().getFirst();
    cg.setAccessManagement(null);
    cg.setGeneralResearchUse(null);
    assertFalse(validator.collectViolations(registration).isEmpty());
  }

  @Test
  void testCollectViolations_dataUse_multiplePrimaryUses() {
    StudyRegistrationRequest registration = createValidRegistration();
    ConsentGroupRequest cg = registration.getConsentGroups().getFirst();
    // valid is OPEN; adding a second primary use makes it invalid
    cg.setGeneralResearchUse(true);
    assertFalse(validator.collectViolations(registration).isEmpty());
  }

  @ParameterizedTest
  @MethodSource("validPrimaryDataUseMutations")
  void testCollectViolations_dataUse_validPrimaryUse(Consumer<ConsentGroupRequest> mutate) {
    StudyRegistrationRequest registration = createValidRegistration();
    ConsentGroupRequest cg = registration.getConsentGroups().getFirst();
    cg.setAccessManagement(null);
    mutate.accept(cg);
    assertTrue(validator.collectViolations(registration).isEmpty());
  }

  static Stream<Consumer<ConsentGroupRequest>> validPrimaryDataUseMutations() {
    return Stream.of(
        cg -> cg.setGeneralResearchUse(true),
        cg -> cg.setHmb(true),
        cg -> cg.setPoa(true),
        cg -> cg.setDiseaseSpecificUse(List.of("DOID:162")),
        cg -> cg.setOtherPrimary("Some specific use"));
  }

  // ── DAC requirement ──────────────────────────────────────────────────────

  @Test
  void testCollectViolations_dac_required_for_controlled() {
    StudyRegistrationRequest registration = createValidRegistration();
    ConsentGroupRequest cg = registration.getConsentGroups().getFirst();
    cg.setAccessManagement(AccessManagement.CONTROLLED);
    cg.setGeneralResearchUse(true);
    cg.setDataAccessCommitteeId(null);
    assertFalse(validator.collectViolations(registration).isEmpty());
  }

  @Test
  void testCollectViolations_dac_not_required_for_open() {
    StudyRegistrationRequest registration = createValidRegistration();
    ConsentGroupRequest cg = registration.getConsentGroups().getFirst();
    // OPEN with no DAC is valid
    cg.setDataAccessCommitteeId(null);
    assertTrue(validator.collectViolations(registration).isEmpty());
  }

  @Test
  void testCollectViolations_dac_provided_for_controlled() {
    StudyRegistrationRequest registration = createValidRegistration();
    ConsentGroupRequest cg = registration.getConsentGroups().getFirst();
    cg.setAccessManagement(AccessManagement.CONTROLLED);
    cg.setGeneralResearchUse(true);
    cg.setDataAccessCommitteeId(RandomUtils.secureStrong().randomInt(1, 100));
    assertTrue(validator.collectViolations(registration).isEmpty());
  }

  // ── NIH conditional fields ───────────────────────────────────────────────

  @Test
  void testCollectViolations_dbGaPPhsID_required() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setNihAnvilUse(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY);
    registration.setDbGaPPhsID(null);
    assertFalse(validator.collectViolations(registration).isEmpty());
  }

  @Test
  void testCollectViolations_piInstitution_required_for_dbgap() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setNihAnvilUse(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY);
    registration.setDbGaPPhsID(RandomStringUtils.secureStrong().nextAlphabetic(8));
    registration.setPiInstitution(null);
    assertFalse(validator.collectViolations(registration).isEmpty());
  }

  @Test
  void testCollectViolations_nihGrantContractNumber_required_for_dbgap() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setNihAnvilUse(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY);
    registration.setDbGaPPhsID(RandomStringUtils.secureStrong().nextAlphabetic(8));
    registration.setPiInstitution(RandomUtils.secureStrong().randomInt(1, 100));
    registration.setNihGrantContractNumber(null);
    assertFalse(validator.collectViolations(registration).isEmpty());
  }

  @Test
  void testCollectViolations_piInstitution_required_for_seeking_anvil() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setNihAnvilUse(
        NihAnvilUse.I_AM_NOT_NHGRI_FUNDED_BUT_I_AM_SEEKING_TO_SUBMIT_DATA_TO_AN_VIL);
    registration.setPiInstitution(null);
    assertFalse(validator.collectViolations(registration).isEmpty());
  }

  @Test
  void testCollectViolations_nihGrantContractNumber_required_for_seeking_anvil() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setNihAnvilUse(
        NihAnvilUse.I_AM_NOT_NHGRI_FUNDED_BUT_I_AM_SEEKING_TO_SUBMIT_DATA_TO_AN_VIL);
    registration.setPiInstitution(RandomUtils.secureStrong().randomInt(1, 100));
    registration.setNihGrantContractNumber(null);
    assertFalse(validator.collectViolations(registration).isEmpty());
  }

  @Test
  void testCollectViolations_piInstitution_required_for_nhgri_no_phs() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setNihAnvilUse(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_DO_NOT_HAVE_A_DB_GA_P_PHS_ID);
    registration.setPiInstitution(null);
    assertFalse(validator.collectViolations(registration).isEmpty());
  }

  // ── NIH happy paths ──────────────────────────────────────────────────────

  @Test
  void testCollectViolations_nihAnvilUse_dbgapPhsId_allFieldsValid() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setNihAnvilUse(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY);
    registration.setDbGaPPhsID(RandomStringUtils.secureStrong().nextAlphabetic(8));
    registration.setPiInstitution(RandomUtils.secureStrong().randomInt(1, 100));
    registration.setNihGrantContractNumber(RandomStringUtils.secureStrong().nextAlphabetic(8));
    assertTrue(validator.collectViolations(registration).isEmpty());
  }

  @Test
  void testCollectViolations_nihAnvilUse_nhgriFundedNoPhs_allFieldsValid() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setNihAnvilUse(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_DO_NOT_HAVE_A_DB_GA_P_PHS_ID);
    registration.setPiInstitution(RandomUtils.secureStrong().randomInt(1, 100));
    registration.setNihGrantContractNumber(RandomStringUtils.secureStrong().nextAlphabetic(8));
    assertTrue(validator.collectViolations(registration).isEmpty());
  }

  // ── GSR conditional ──────────────────────────────────────────────────────

  @Test
  void testCollectViolations_gsrExplanation_required_when_gsr_true() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setControlledAccessRequiredForGenomicSummaryResultsGSR(true);
    registration.setControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation(null);
    assertFalse(validator.collectViolations(registration).isEmpty());
  }

  @Test
  void testCollectViolations_gsrExplanation_not_required_when_gsr_false() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setControlledAccessRequiredForGenomicSummaryResultsGSR(false);
    registration.setControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation(null);
    assertTrue(validator.collectViolations(registration).isEmpty());
  }

  @Test
  void testCollectViolations_gsrExplanation_present_when_gsr_true() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setControlledAccessRequiredForGenomicSummaryResultsGSR(true);
    registration.setControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation(
        RandomStringUtils.secureStrong().nextAlphabetic(10));
    assertTrue(validator.collectViolations(registration).isEmpty());
  }

  @Test
  void testCollectViolations_gsrExplanation_blank_when_gsr_true() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setControlledAccessRequiredForGenomicSummaryResultsGSR(true);
    registration.setControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation("   ");
    assertFalse(validator.collectViolations(registration).isEmpty());
  }

  // ── Alt sharing plan ─────────────────────────────────────────────────────

  @Test
  void testCollectViolations_altSharingPlanExplanation_required() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setAlternativeDataSharingPlan(true);
    registration.setAlternativeDataSharingPlanExplanation(null);
    assertFalse(validator.collectViolations(registration).isEmpty());
  }

  @Test
  void testCollectViolations_altSharingPlanReasons_required() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setAlternativeDataSharingPlan(true);
    registration.setAlternativeDataSharingPlanExplanation(
        RandomStringUtils.secureStrong().nextAlphabetic(10));
    registration.setAlternativeDataSharingPlanReasons(List.of());
    assertFalse(validator.collectViolations(registration).isEmpty());
  }

  @Test
  void testCollectViolations_altSharingPlanReasons_null() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setAlternativeDataSharingPlan(true);
    registration.setAlternativeDataSharingPlanExplanation(
        RandomStringUtils.secureStrong().nextAlphabetic(10));
    registration.setAlternativeDataSharingPlanReasons(null);
    assertFalse(validator.collectViolations(registration).isEmpty());
  }

  @Test
  void testCollectViolations_altSharingPlan_valid() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setAlternativeDataSharingPlan(true);
    registration.setAlternativeDataSharingPlanExplanation(
        RandomStringUtils.secureStrong().nextAlphabetic(10));
    registration.setAlternativeDataSharingPlanReasons(
        List.of(AlternativeDataSharingPlanReason.OTHER));
    assertTrue(validator.collectViolations(registration).isEmpty());
  }

  // ── Email validation ─────────────────────────────────────────────────────

  @Test
  void testCollectViolations_piEmail_invalid() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setPiEmail("not-an-email");
    assertFalse(validator.collectViolations(registration).isEmpty());
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"   ", "pi@example.com"})
  void testCollectViolations_piEmail_allowed(String email) {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setPiEmail(email);
    assertTrue(validator.collectViolations(registration).isEmpty());
  }

  @Test
  void testCollectViolations_dataCustodianEmail_invalid() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setDataCustodianEmail(List.of("valid@example.com", "not-an-email"));
    assertFalse(validator.collectViolations(registration).isEmpty());
  }

  @Test
  void testCollectViolations_dataCustodianEmail_valid() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setDataCustodianEmail(List.of("a@example.com", "b@example.org"));
    assertTrue(validator.collectViolations(registration).isEmpty());
  }

  @Test
  void testCollectViolations_dataCustodianEmail_blank_is_filtered() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setDataCustodianEmail(List.of("  ", "valid@example.com"));
    assertTrue(validator.collectViolations(registration).isEmpty());
  }

  // ── Date validation ──────────────────────────────────────────────────────

  static Stream<Consumer<StudyRegistrationRequest>> invalidDateMutations() {
    return Stream.of(
        r -> r.setEmbargoReleaseDate("01/15/2025"),
        r -> r.setAlternativeDataSharingPlanTargetDeliveryDate("not-a-date"),
        r -> r.setAlternativeDataSharingPlanTargetPublicReleaseDate("15-01-2025"),
        r -> r.getConsentGroups().getFirst().setMorDate("January 15, 2025"));
  }

  @Test
  void testCollectViolations_embargoReleaseDate_valid() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setEmbargoReleaseDate("2025-01-15");
    assertTrue(validator.collectViolations(registration).isEmpty());
  }

  @Test
  void testCollectViolations_embargoReleaseDate_blank_is_allowed() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setEmbargoReleaseDate("  ");
    assertTrue(validator.collectViolations(registration).isEmpty());
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"2025-06-01"})
  void testCollectViolations_morDate_allowed(String date) {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.getConsentGroups().getFirst().setMorDate(date);
    assertTrue(validator.collectViolations(registration).isEmpty());
  }

  // ── Data-use edge cases ───────────────────────────────────────────────────

  @Test
  void testCollectViolations_diseaseSpecificUse_emptyList_notCounted() {
    StudyRegistrationRequest registration = createValidRegistration();
    ConsentGroupRequest cg = registration.getConsentGroups().getFirst();
    // OPEN access is already set; empty diseaseSpecificUse adds nothing → count=1, valid
    cg.setDiseaseSpecificUse(new ArrayList<>());
    assertTrue(validator.collectViolations(registration).isEmpty());
  }

  @Test
  void testCollectViolations_otherPrimary_blank_not_counted() {
    StudyRegistrationRequest registration = createValidRegistration();
    ConsentGroupRequest cg = registration.getConsentGroups().getFirst();
    // OPEN access is already set; blank otherPrimary adds nothing → count=1, valid
    cg.setOtherPrimary("   ");
    assertTrue(validator.collectViolations(registration).isEmpty());
  }

  // ── collectViolations (aggregating multiple violations at once) ──────────

  @Test
  void testCollectViolations_multipleTopLevelViolations_allReported() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setStudyName(null);
    registration.setDataTypes(List.of());
    registration.setPiName(null);

    List<String> violations = validator.collectViolations(registration);

    assertTrue(violations.contains("Study Name is required"));
    assertTrue(violations.contains("Data Types is required"));
    assertTrue(violations.contains("Principal Investigator Name is required"));
    assertTrue(violations.size() >= 3);
  }

  @Test
  void testCollectViolations_multipleConsentGroups_bothReported() {
    StudyRegistrationRequest registration = createValidRegistration();
    ConsentGroupRequest missingName = createValidConsentGroup();
    missingName.setConsentGroupName(null);
    ConsentGroupRequest missingParticipants = createValidConsentGroup();
    missingParticipants.setNumberOfParticipants(null);
    registration.setConsentGroups(List.of(missingName, missingParticipants));

    List<String> violations = validator.collectViolations(registration);

    assertTrue(violations.contains("Dataset Name is required"));
    assertTrue(violations.contains("Number of Participants is required"));
  }

  @Test
  void testCollectViolations_nihAnvilConditionalFields_allReported() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setNihAnvilUse(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY);
    registration.setDbGaPPhsID(null);
    registration.setPiInstitution(null);
    registration.setNihGrantContractNumber(null);

    List<String> violations = validator.collectViolations(registration);

    assertTrue(violations.contains("dbGaP phs ID is required"));
    assertTrue(violations.contains("Principal Investigator Institution is required"));
    assertTrue(violations.contains("NIH Grant or Contract Number is required"));
  }

  @Test
  void testCollectViolations_missingNihAnvilUse_doesNotThrow() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setStudyName(null);
    registration.setNihAnvilUse(null);

    List<String> violations = assertDoesNotThrow(() -> validator.collectViolations(registration));

    assertTrue(violations.contains("Study Name is required"));
    assertTrue(violations.contains("NIH Anvil Use is required"));
  }

  @Test
  void testCollectViolations_invalidEmails_bothReported() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setPiEmail("not-an-email");
    registration.setDataCustodianEmail(List.of("also-not-an-email"));

    List<String> violations = validator.collectViolations(registration);

    assertTrue(violations.contains("PI Email is not a valid email address"));
    assertTrue(
        violations.contains(
            "Data Custodian Email is not a valid email address: also-not-an-email"));
  }

  // ── Individual check* methods (@VisibleForTesting) ────────────────────────

  @Test
  void testCheckStudyNameRequired_missing() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setStudyName(null);
    assertThrows(BadRequestException.class, () -> validator.checkStudyNameRequired(registration));
  }

  @Test
  void testCheckStudyNameRequired_present() {
    StudyRegistrationRequest registration = createValidRegistration();
    assertDoesNotThrow(() -> validator.checkStudyNameRequired(registration));
  }

  @Test
  void testCheckStudyDescriptionRequired_missing() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setStudyDescription(null);
    assertThrows(
        BadRequestException.class, () -> validator.checkStudyDescriptionRequired(registration));
  }

  @Test
  void testCheckStudyDescriptionRequired_present() {
    StudyRegistrationRequest registration = createValidRegistration();
    assertDoesNotThrow(() -> validator.checkStudyDescriptionRequired(registration));
  }

  @Test
  void testCheckStudyDescriptionRequired_blank() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setStudyDescription("   ");
    assertThrows(
        BadRequestException.class, () -> validator.checkStudyDescriptionRequired(registration));
  }

  @Test
  void testCheckDataTypesRequired_missing() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setDataTypes(List.of());
    assertThrows(BadRequestException.class, () -> validator.checkDataTypesRequired(registration));
  }

  @Test
  void testCheckDataTypesRequired_present() {
    StudyRegistrationRequest registration = createValidRegistration();
    assertDoesNotThrow(() -> validator.checkDataTypesRequired(registration));
  }

  @Test
  void testCheckPublicVisibilityRequired_missing() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setPublicVisibility(null);
    assertThrows(
        BadRequestException.class, () -> validator.checkPublicVisibilityRequired(registration));
  }

  @Test
  void testCheckPublicVisibilityRequired_present() {
    StudyRegistrationRequest registration = createValidRegistration();
    assertDoesNotThrow(() -> validator.checkPublicVisibilityRequired(registration));
  }

  @Test
  void testCheckNihAnvilUseRequired_missing() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setNihAnvilUse(null);
    assertThrows(BadRequestException.class, () -> validator.checkNihAnvilUseRequired(registration));
  }

  @Test
  void testCheckNihAnvilUseRequired_present() {
    StudyRegistrationRequest registration = createValidRegistration();
    assertDoesNotThrow(() -> validator.checkNihAnvilUseRequired(registration));
  }

  @Test
  void testCheckPiNameRequired_missing() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setPiName(null);
    assertThrows(BadRequestException.class, () -> validator.checkPiNameRequired(registration));
  }

  @Test
  void testCheckPiNameRequired_present() {
    StudyRegistrationRequest registration = createValidRegistration();
    assertDoesNotThrow(() -> validator.checkPiNameRequired(registration));
  }

  @Test
  void testCheckPiNameRequired_blank() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setPiName("   ");
    assertThrows(BadRequestException.class, () -> validator.checkPiNameRequired(registration));
  }

  @Test
  void testCheckDbGaPPhsIdRequired_missing() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setDbGaPPhsID(null);
    assertThrows(BadRequestException.class, () -> validator.checkDbGaPPhsIdRequired(registration));
  }

  @Test
  void testCheckDbGaPPhsIdRequired_present() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setDbGaPPhsID(RandomStringUtils.secureStrong().nextAlphabetic(8));
    assertDoesNotThrow(() -> validator.checkDbGaPPhsIdRequired(registration));
  }

  @Test
  void testCheckDbGaPPhsIdRequired_blank() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setDbGaPPhsID("   ");
    assertThrows(BadRequestException.class, () -> validator.checkDbGaPPhsIdRequired(registration));
  }

  @Test
  void testCheckPiInstitutionRequired_missing() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setPiInstitution(null);
    assertThrows(
        BadRequestException.class, () -> validator.checkPiInstitutionRequired(registration));
  }

  @Test
  void testCheckPiInstitutionRequired_present() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setPiInstitution(RandomUtils.secureStrong().randomInt(1, 100));
    assertDoesNotThrow(() -> validator.checkPiInstitutionRequired(registration));
  }

  @Test
  void testCheckNihGrantContractNumberRequired_missing() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setNihGrantContractNumber(null);
    assertThrows(
        BadRequestException.class,
        () -> validator.checkNihGrantContractNumberRequired(registration));
  }

  @Test
  void testCheckNihGrantContractNumberRequired_present() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setNihGrantContractNumber(RandomStringUtils.secureStrong().nextAlphabetic(8));
    assertDoesNotThrow(() -> validator.checkNihGrantContractNumberRequired(registration));
  }

  @Test
  void testCheckNihGrantContractNumberRequired_blank() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setNihGrantContractNumber("   ");
    assertThrows(
        BadRequestException.class,
        () -> validator.checkNihGrantContractNumberRequired(registration));
  }

  @Test
  void testCheckAltDataSharingExplanationRequired_missing() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setAlternativeDataSharingPlanExplanation(null);
    assertThrows(
        BadRequestException.class,
        () -> validator.checkAltDataSharingExplanationRequired(registration));
  }

  @Test
  void testCheckAltDataSharingExplanationRequired_present() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setAlternativeDataSharingPlanExplanation(
        RandomStringUtils.secureStrong().nextAlphabetic(10));
    assertDoesNotThrow(() -> validator.checkAltDataSharingExplanationRequired(registration));
  }

  @Test
  void testCheckAltDataSharingReasonsRequired_missing() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setAlternativeDataSharingPlanReasons(List.of());
    assertThrows(
        BadRequestException.class,
        () -> validator.checkAltDataSharingReasonsRequired(registration));
  }

  @Test
  void testCheckAltDataSharingReasonsRequired_present() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setAlternativeDataSharingPlanReasons(
        List.of(AlternativeDataSharingPlanReason.OTHER));
    assertDoesNotThrow(() -> validator.checkAltDataSharingReasonsRequired(registration));
  }

  @Test
  void testCheckPiEmailValid_invalid() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setPiEmail("not-an-email");
    assertThrows(BadRequestException.class, () -> validator.checkPiEmailValid(registration));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"   ", "pi@example.com"})
  void testCheckPiEmailValid_allowed(String email) {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setPiEmail(email);
    assertDoesNotThrow(() -> validator.checkPiEmailValid(registration));
  }

  @Test
  void testCheckDataCustodianEmailsValid_invalid() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setDataCustodianEmail(List.of("valid@example.com", "not-an-email"));
    assertThrows(
        BadRequestException.class, () -> validator.checkDataCustodianEmailsValid(registration));
  }

  @Test
  void testCheckDataCustodianEmailsValid_valid() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setDataCustodianEmail(List.of("a@example.com", "b@example.org"));
    assertDoesNotThrow(() -> validator.checkDataCustodianEmailsValid(registration));
  }

  @Test
  void testCheckConsentGroupNameRequired_missing() {
    ConsentGroupRequest cg = createValidConsentGroup();
    cg.setConsentGroupName(null);
    assertThrows(BadRequestException.class, () -> validator.checkConsentGroupNameRequired(cg));
  }

  @Test
  void testCheckConsentGroupNameRequired_present() {
    ConsentGroupRequest cg = createValidConsentGroup();
    assertDoesNotThrow(() -> validator.checkConsentGroupNameRequired(cg));
  }

  @Test
  void testCheckNumberOfParticipantsRequired_missing() {
    ConsentGroupRequest cg = createValidConsentGroup();
    cg.setNumberOfParticipants(null);
    assertThrows(BadRequestException.class, () -> validator.checkNumberOfParticipantsRequired(cg));
  }

  @Test
  void testCheckNumberOfParticipantsRequired_present() {
    ConsentGroupRequest cg = createValidConsentGroup();
    assertDoesNotThrow(() -> validator.checkNumberOfParticipantsRequired(cg));
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
}
