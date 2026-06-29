package org.broadinstitute.consent.http.models.dto.registration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.BadRequestException;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.NihAnvilUse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

  // ── Required study fields ────────────────────────────────────────────────

  @Test
  void testValidate_studyName_null() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setStudyName(null);
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_studyName_blank() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setStudyName("  ");
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_studyDescription_null() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setStudyDescription(null);
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_dataTypes_null() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setDataTypes(null);
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_dataTypes_empty() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setDataTypes(List.of());
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_publicVisibility_null() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setPublicVisibility(null);
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_nihAnvilUse_null() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setNihAnvilUse(null);
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_piName_null() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setPiName(null);
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  // ── Consent groups ───────────────────────────────────────────────────────

  @Test
  void testValidate_consentGroups_null() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setConsentGroups(null);
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_consentGroups_empty() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setConsentGroups(List.of());
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_consentGroupName_null() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.getConsentGroups().get(0).setConsentGroupName(null);
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_consentGroupName_blank() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.getConsentGroups().get(0).setConsentGroupName("   ");
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_numberOfParticipants_null() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.getConsentGroups().get(0).setNumberOfParticipants(null);
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
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

  @Test
  void testValidate_dataUse_generalResearchUse() {
    StudyRegistrationRequest registration = createValidRegistration();
    ConsentGroupRequest cg = registration.getConsentGroups().get(0);
    cg.setAccessManagement(null);
    cg.setGeneralResearchUse(true);
    assertDoesNotThrow(() -> validator.validate(registration));
  }

  @Test
  void testValidate_dataUse_hmb() {
    StudyRegistrationRequest registration = createValidRegistration();
    ConsentGroupRequest cg = registration.getConsentGroups().get(0);
    cg.setAccessManagement(null);
    cg.setHmb(true);
    assertDoesNotThrow(() -> validator.validate(registration));
  }

  @Test
  void testValidate_dataUse_poa() {
    StudyRegistrationRequest registration = createValidRegistration();
    ConsentGroupRequest cg = registration.getConsentGroups().get(0);
    cg.setAccessManagement(null);
    cg.setPoa(true);
    assertDoesNotThrow(() -> validator.validate(registration));
  }

  @Test
  void testValidate_dataUse_diseaseSpecificUse() {
    StudyRegistrationRequest registration = createValidRegistration();
    ConsentGroupRequest cg = registration.getConsentGroups().get(0);
    cg.setAccessManagement(null);
    cg.setDiseaseSpecificUse(List.of("DOID:162"));
    assertDoesNotThrow(() -> validator.validate(registration));
  }

  @Test
  void testValidate_dataUse_otherPrimary() {
    StudyRegistrationRequest registration = createValidRegistration();
    ConsentGroupRequest cg = registration.getConsentGroups().get(0);
    cg.setAccessManagement(null);
    cg.setOtherPrimary("Some specific use");
    assertDoesNotThrow(() -> validator.validate(registration));
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
    cg.setDataAccessCommitteeId(RandomUtils.nextInt(1, 100));
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
    registration.setDbGaPPhsID(RandomStringUtils.randomAlphabetic(8));
    registration.setPiInstitution(null);
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_nihGrantContractNumber_required_for_dbgap() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setNihAnvilUse(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY);
    registration.setDbGaPPhsID(RandomStringUtils.randomAlphabetic(8));
    registration.setPiInstitution(RandomUtils.nextInt(1, 100));
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
    registration.setPiInstitution(RandomUtils.nextInt(1, 100));
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
    registration.setAlternativeDataSharingPlanExplanation(RandomStringUtils.randomAlphabetic(10));
    registration.setAlternativeDataSharingPlanReasons(List.of());
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  // ── Email validation ─────────────────────────────────────────────────────

  @Test
  void testValidate_piEmail_invalid() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setPiEmail("not-an-email");
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_piEmail_valid() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setPiEmail("pi@example.com");
    assertDoesNotThrow(() -> validator.validate(registration));
  }

  @Test
  void testValidate_piEmail_absent_is_allowed() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setPiEmail(null);
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

  // ── Date validation ──────────────────────────────────────────────────────

  @Test
  void testValidate_embargoReleaseDate_invalid() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setEmbargoReleaseDate("01/15/2025");
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_embargoReleaseDate_valid() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setEmbargoReleaseDate("2025-01-15");
    assertDoesNotThrow(() -> validator.validate(registration));
  }

  @Test
  void testValidate_altSharingDeliveryDate_invalid() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setAlternativeDataSharingPlanTargetDeliveryDate("not-a-date");
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_altSharingPublicReleaseDate_invalid() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.setAlternativeDataSharingPlanTargetPublicReleaseDate("15-01-2025");
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_morDate_invalid() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.getConsentGroups().get(0).setMorDate("January 15, 2025");
    assertThrows(BadRequestException.class, () -> validator.validate(registration));
  }

  @Test
  void testValidate_morDate_valid() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.getConsentGroups().get(0).setMorDate("2025-06-01");
    assertDoesNotThrow(() -> validator.validate(registration));
  }

  @Test
  void testValidate_morDate_absent_is_allowed() {
    StudyRegistrationRequest registration = createValidRegistration();
    registration.getConsentGroups().get(0).setMorDate(null);
    assertDoesNotThrow(() -> validator.validate(registration));
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  private ConsentGroupRequest createValidConsentGroup() {
    ConsentGroupRequest cg = new ConsentGroupRequest();
    cg.setConsentGroupName(RandomStringUtils.randomAlphabetic(10));
    cg.setNumberOfParticipants(RandomUtils.nextInt(1, 100));
    cg.setAccessManagement(AccessManagement.OPEN);
    return cg;
  }

  private StudyRegistrationRequest createValidRegistration() {
    StudyRegistrationRequest registration = new StudyRegistrationRequest();
    registration.setStudyName(RandomStringUtils.randomAlphabetic(10));
    registration.setStudyDescription(RandomStringUtils.randomAlphabetic(20));
    registration.setDataTypes(List.of(RandomStringUtils.randomAlphabetic(8)));
    registration.setPublicVisibility(true);
    registration.setNihAnvilUse(
        NihAnvilUse.I_AM_NOT_NHGRI_FUNDED_AND_DO_NOT_PLAN_TO_STORE_DATA_IN_AN_VIL);
    registration.setPiName(RandomStringUtils.randomAlphabetic(10));
    registration.setConsentGroups(List.of(createValidConsentGroup()));
    return registration;
  }
}
