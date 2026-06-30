package org.broadinstitute.consent.http.models.dto.registration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
  void testValidate_valid() {
    StudyRegistrationRequest registration = createValidRegistration();
    assertTrue(validator.validate(registration));
  }

  // ── Required fields & invalid values ─────────────────────────────────────

  @ParameterizedTest
  @MethodSource({"invalidRegistrationMutations", "invalidDateMutations"})
  void testValidate_invalidField_throws(Consumer<StudyRegistrationRequest> mutate) {
    StudyRegistrationRequest registration = createValidRegistration();
    mutate.accept(registration);
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  static Stream<Consumer<StudyRegistrationRequest>> invalidRegistrationMutations() {
    return Stream.<Consumer<StudyRegistrationRequest>>of(
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
        r -> r.getConsentGroups().get(0).setConsentGroupName(null),
        r -> r.getConsentGroups().get(0).setConsentGroupName("   "),
        r -> r.getConsentGroups().get(0).setNumberOfParticipants(null));
  }

  // ── Data-use consistency ─────────────────────────────────────────────────

  @Test
  void testValidate_dataUse_noPrimaryUse() {
    StudyRegistrationRequest registration = createValidRegistration();
    ConsentGroupRequest cg = registration.getConsentGroups().get(0);
    cg.setAccessManagement(null);
    cg.setGeneralResearchUse(null);
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_dataUse_multiplePrimaryUses() {
    StudyRegistrationRequest registration = createValidRegistration();
    ConsentGroupRequest cg = registration.getConsentGroups().get(0);
    // valid is OPEN; adding a second primary use makes it invalid
    cg.setGeneralResearchUse(true);
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @ParameterizedTest
  @MethodSource("validPrimaryDataUseMutations")
  void testValidate_dataUse_validPrimaryUse(Consumer<ConsentGroupRequest> mutate) {
    StudyRegistrationRequest registration = createValidRegistration();
    ConsentGroupRequest cg = registration.getConsentGroups().get(0);
    cg.setAccessManagement(null);
    mutate.accept(cg);
    assertDoesNotThrow(() -> validator.validate(registration));
  }

  static Stream<Consumer<ConsentGroupRequest>> validPrimaryDataUseMutations() {
    return Stream.<Consumer<ConsentGroupRequest>>of(
        cg -> cg.setGeneralResearchUse(true),
        cg -> cg.setHmb(true),
        cg -> cg.setPoa(true),
        cg -> cg.setDiseaseSpecificUse(List.of("DOID:162")),
        cg -> cg.setOtherPrimary("Some specific use"));
  }

  // ── DAC requirement ──────────────────────────────────────────────────────

  @Test
  void testValidate_dac_required_for_controlled() {
    StudyRegistrationRequest registration = createValidRegistration();
    ConsentGroupRequest cg = registration.getConsentGroups().get(0);
    cg.setAccessManagement(AccessManagement.CONTROLLED);
    cg.setGeneralResearchUse(true);
    cg.setDataAccessCommitteeId(null);
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_dac_not_required_for_open() {
    StudyRegistrationRequest registration = createValidRegistration();
    ConsentGroupRequest cg = registration.getConsentGroups().get(0);
    // OPEN with no DAC is valid
    cg.setDataAccessCommitteeId(null);
    assertDoesNotThrow(() -> validator.validate(registration));
  }

  @Test
  void testValidate_dac_provided_for_controlled() {
    StudyRegistrationRequest registration = createValidRegistration();
    ConsentGroupRequest cg = registration.getConsentGroups().get(0);
    cg.setAccessManagement(AccessManagement.CONTROLLED);
    cg.setGeneralResearchUse(true);
    cg.setDataAccessCommitteeId(RandomUtils.secureStrong().randomInt(1, 100));
    assertDoesNotThrow(() -> validator.validate(registration));
  }

  // ── NIH conditional fields ───────────────────────────────────────────────

  @Test
  void testValidate_dbGaPPhsID_required() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setNihAnvilUse(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY);
    registration.setDbGaPPhsID(null);
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_piInstitution_required_for_dbgap() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setNihAnvilUse(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY);
    registration.setDbGaPPhsID(RandomStringUtils.secureStrong().nextAlphabetic(8));
    registration.setPiInstitution(null);
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_nihGrantContractNumber_required_for_dbgap() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setNihAnvilUse(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY);
    registration.setDbGaPPhsID(RandomStringUtils.secureStrong().nextAlphabetic(8));
    registration.setPiInstitution(RandomUtils.secureStrong().randomInt(1, 100));
    registration.setNihGrantContractNumber(null);
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_piInstitution_required_for_seeking_anvil() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setNihAnvilUse(
        NihAnvilUse.I_AM_NOT_NHGRI_FUNDED_BUT_I_AM_SEEKING_TO_SUBMIT_DATA_TO_AN_VIL);
    registration.setPiInstitution(null);
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_nihGrantContractNumber_required_for_seeking_anvil() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setNihAnvilUse(
        NihAnvilUse.I_AM_NOT_NHGRI_FUNDED_BUT_I_AM_SEEKING_TO_SUBMIT_DATA_TO_AN_VIL);
    registration.setPiInstitution(RandomUtils.secureStrong().randomInt(1, 100));
    registration.setNihGrantContractNumber(null);
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_piInstitution_required_for_nhgri_no_phs() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setNihAnvilUse(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_DO_NOT_HAVE_A_DB_GA_P_PHS_ID);
    registration.setPiInstitution(null);
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  // ── NIH happy paths ──────────────────────────────────────────────────────

  @Test
  void testValidate_nihAnvilUse_dbgapPhsId_allFieldsValid() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setNihAnvilUse(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY);
    registration.setDbGaPPhsID(RandomStringUtils.secureStrong().nextAlphabetic(8));
    registration.setPiInstitution(RandomUtils.secureStrong().randomInt(1, 100));
    registration.setNihGrantContractNumber(RandomStringUtils.secureStrong().nextAlphabetic(8));
    assertDoesNotThrow(() -> validator.validate(registration));
  }

  @Test
  void testValidate_nihAnvilUse_nhgriFundedNoPhs_allFieldsValid() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setNihAnvilUse(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_DO_NOT_HAVE_A_DB_GA_P_PHS_ID);
    registration.setPiInstitution(RandomUtils.secureStrong().randomInt(1, 100));
    registration.setNihGrantContractNumber(RandomStringUtils.secureStrong().nextAlphabetic(8));
    assertDoesNotThrow(() -> validator.validate(registration));
  }

  // ── GSR conditional ──────────────────────────────────────────────────────

  @Test
  void testValidate_gsrExplanation_required_when_gsr_true() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setControlledAccessRequiredForGenomicSummaryResultsGSR(true);
    registration.setControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation(null);
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_gsrExplanation_not_required_when_gsr_false() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setControlledAccessRequiredForGenomicSummaryResultsGSR(false);
    registration.setControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation(null);
    assertDoesNotThrow(() -> validator.validate(registration));
  }

  @Test
  void testValidate_gsrExplanation_present_when_gsr_true() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setControlledAccessRequiredForGenomicSummaryResultsGSR(true);
    registration.setControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation(
        RandomStringUtils.secureStrong().nextAlphabetic(10));
    assertDoesNotThrow(() -> validator.validate(registration));
  }

  // ── Alt sharing plan ─────────────────────────────────────────────────────

  @Test
  void testValidate_altSharingPlanExplanation_required() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setAlternativeDataSharingPlan(true);
    registration.setAlternativeDataSharingPlanExplanation(null);
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_altSharingPlanReasons_required() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setAlternativeDataSharingPlan(true);
    registration.setAlternativeDataSharingPlanExplanation(
        RandomStringUtils.secureStrong().nextAlphabetic(10));
    registration.setAlternativeDataSharingPlanReasons(List.of());
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_altSharingPlanReasons_null() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setAlternativeDataSharingPlan(true);
    registration.setAlternativeDataSharingPlanExplanation(
        RandomStringUtils.secureStrong().nextAlphabetic(10));
    registration.setAlternativeDataSharingPlanReasons(null);
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_altSharingPlan_valid() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setAlternativeDataSharingPlan(true);
    registration.setAlternativeDataSharingPlanExplanation(
        RandomStringUtils.secureStrong().nextAlphabetic(10));
    registration.setAlternativeDataSharingPlanReasons(
        List.of(AlternativeDataSharingPlanReason.OTHER));
    assertDoesNotThrow(() -> validator.validate(registration));
  }

  // ── Email validation ─────────────────────────────────────────────────────

  @Test
  void testValidate_piEmail_invalid() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setPiEmail("not-an-email");
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"   ", "pi@example.com"})
  void testValidate_piEmail_allowed(String email) {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setPiEmail(email);
    assertDoesNotThrow(() -> validator.validate(registration));
  }

  @Test
  void testValidate_dataCustodianEmail_invalid() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setDataCustodianEmail(List.of("valid@example.com", "not-an-email"));
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_dataCustodianEmail_valid() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setDataCustodianEmail(List.of("a@example.com", "b@example.org"));
    assertDoesNotThrow(() -> validator.validate(registration));
  }

  @Test
  void testValidate_dataCustodianEmail_blank_is_filtered() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setDataCustodianEmail(List.of("  ", "valid@example.com"));
    assertDoesNotThrow(() -> validator.validate(registration));
  }

  // ── Date validation ──────────────────────────────────────────────────────

  static Stream<Consumer<StudyRegistrationRequest>> invalidDateMutations() {
    return Stream.<Consumer<StudyRegistrationRequest>>of(
        r -> r.setEmbargoReleaseDate("01/15/2025"),
        r -> r.setAlternativeDataSharingPlanTargetDeliveryDate("not-a-date"),
        r -> r.setAlternativeDataSharingPlanTargetPublicReleaseDate("15-01-2025"),
        r -> r.getConsentGroups().get(0).setMorDate("January 15, 2025"));
  }

  @Test
  void testValidate_embargoReleaseDate_valid() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setEmbargoReleaseDate("2025-01-15");
    assertDoesNotThrow(() -> validator.validate(registration));
  }

  @Test
  void testValidate_embargoReleaseDate_blank_is_allowed() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setEmbargoReleaseDate("  ");
    assertDoesNotThrow(() -> validator.validate(registration));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"2025-06-01"})
  void testValidate_morDate_allowed(String date) {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.getConsentGroups().get(0).setMorDate(date);
    assertDoesNotThrow(() -> validator.validate(registration));
  }

  // ── Data-use edge cases ───────────────────────────────────────────────────

  @Test
  void testValidate_diseaseSpecificUse_emptyList_notCounted() {
    StudyRegistrationRequest registration = createValidRegistration();
    ConsentGroupRequest cg = registration.getConsentGroups().get(0);
    // OPEN access is already set; empty diseaseSpecificUse adds nothing → count=1, valid
    cg.setDiseaseSpecificUse(new ArrayList<>());
    assertDoesNotThrow(() -> validator.validate(registration));
  }

  @Test
  void testValidate_otherPrimary_blank_not_counted() {
    StudyRegistrationRequest registration = createValidRegistration();
    ConsentGroupRequest cg = registration.getConsentGroups().get(0);
    // OPEN access is already set; blank otherPrimary adds nothing → count=1, valid
    cg.setOtherPrimary("   ");
    assertDoesNotThrow(() -> validator.validate(registration));
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
