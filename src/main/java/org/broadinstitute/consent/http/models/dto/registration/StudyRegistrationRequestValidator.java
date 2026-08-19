package org.broadinstitute.consent.http.models.dto.registration;

import com.google.common.annotations.VisibleForTesting;
import jakarta.ws.rs.BadRequestException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.validator.routines.EmailValidator;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.NihAnvilUse;
import org.broadinstitute.consent.http.models.datause.DataUsePrimaryClassifier;
import org.broadinstitute.consent.http.models.datause.DataUsePrimaryValidator;

public class StudyRegistrationRequestValidator {

  /**
   * Collects every violation instead of throwing on the first one. Used by the create endpoint,
   * which must report all problems in a single response.
   */
  public List<String> collectViolations(StudyRegistrationRequest registration) {
    Set<String> errors = new LinkedHashSet<>(collectStudyViolations(registration));
    consentGroups(registration).forEach(cg -> errors.addAll(collectConsentGroupViolations(cg)));
    return new ArrayList<>(errors);
  }

  /**
   * The study-scoped violations, including the requirement that the study have a dataset at all.
   * Offered separately from {@link #collectConsentGroupViolations} so a caller that needs to say
   * where a violation came from can attribute each scope on its own; consent groups otherwise share
   * a message set, and two datasets missing the same field are indistinguishable.
   */
  public List<String> collectStudyViolations(StudyRegistrationRequest registration) {
    Set<String> errors = new LinkedHashSet<>();
    collectRequiredStudyFieldViolations(registration, errors);
    collectConditionalFieldViolations(registration, errors);
    collectEmailFieldViolations(registration, errors);
    collectDateFieldViolations(registration, errors);
    if (consentGroups(registration).isEmpty()) {
      errors.add("At least one Dataset is required");
    }
    return new ArrayList<>(errors);
  }

  /** The violations of one consent group, independent of its siblings. */
  public List<String> collectConsentGroupViolations(ConsentGroupRequest cg) {
    Set<String> errors = new LinkedHashSet<>();
    collectTry(errors, () -> checkConsentGroupNameRequired(cg));
    collectTry(errors, () -> checkNumberOfParticipantsRequired(cg));
    collectTry(errors, () -> validateDataUseConsistency(cg));
    collectTry(errors, () -> validateDacRequirement(cg));
    collectTry(errors, () -> validateDateString(cg.getMorDate(), "Moratorium Date"));
    return new ArrayList<>(errors);
  }

  private static List<ConsentGroupRequest> consentGroups(StudyRegistrationRequest registration) {
    return Objects.isNull(registration.getConsentGroups())
        ? List.of()
        : registration.getConsentGroups();
  }

  private static void collectTry(Set<String> errors, Runnable check) {
    try {
      check.run();
    } catch (BadRequestException e) {
      errors.add(e.getMessage());
    }
  }

  private void collectRequiredStudyFieldViolations(StudyRegistrationRequest r, Set<String> errors) {
    collectTry(errors, () -> checkStudyNameRequired(r));
    collectTry(errors, () -> checkStudyDescriptionRequired(r));
    collectTry(errors, () -> checkDataTypesRequired(r));
    collectTry(errors, () -> checkPublicVisibilityRequired(r));
    collectTry(errors, () -> checkNihAnvilUseRequired(r));
    collectTry(errors, () -> checkPiNameRequired(r));
  }

  @VisibleForTesting
  protected void checkStudyNameRequired(StudyRegistrationRequest r) {
    if (Objects.isNull(r.getStudyName()) || r.getStudyName().isBlank()) {
      throw new BadRequestException("Study Name is required");
    }
  }

  protected void checkStudyDescriptionRequired(StudyRegistrationRequest r) {
    if (Objects.isNull(r.getStudyDescription()) || r.getStudyDescription().isBlank()) {
      throw new BadRequestException("Study Description is required");
    }
  }

  protected void checkDataTypesRequired(StudyRegistrationRequest r) {
    if (Objects.isNull(r.getDataTypes()) || r.getDataTypes().isEmpty()) {
      throw new BadRequestException("Data Types is required");
    }
  }

  protected void checkPublicVisibilityRequired(StudyRegistrationRequest r) {
    if (Objects.isNull(r.getPublicVisibility())) {
      throw new BadRequestException("Public Visibility is required");
    }
  }

  protected void checkNihAnvilUseRequired(StudyRegistrationRequest r) {
    if (Objects.isNull(r.getNihAnvilUse())) {
      throw new BadRequestException("NIH Anvil Use is required");
    }
  }

  protected void checkPiNameRequired(StudyRegistrationRequest r) {
    if (Objects.isNull(r.getPiName()) || r.getPiName().isBlank()) {
      throw new BadRequestException("Principal Investigator Name is required");
    }
  }

  protected void validateConditionalFields(StudyRegistrationRequest registration) {
    validateNihAnvilUseConditionals(registration.getNihAnvilUse(), registration);
    validateGsrConditionals(registration);
    validateAlternativeDataSharingConditionals(registration);
  }

  private void collectConditionalFieldViolations(StudyRegistrationRequest r, Set<String> errors) {
    NihAnvilUse anvilUse = r.getNihAnvilUse();
    if (anvilUse != null) {
      if (anvilUse.equals(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY)) {
        collectTry(errors, () -> checkDbGaPPhsIdRequired(r));
        collectTry(errors, () -> checkPiInstitutionRequired(r));
        collectTry(errors, () -> checkNihGrantContractNumberRequired(r));
      }
      if (anvilUse.equals(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_DO_NOT_HAVE_A_DB_GA_P_PHS_ID)
          || anvilUse.equals(
              NihAnvilUse.I_AM_NOT_NHGRI_FUNDED_BUT_I_AM_SEEKING_TO_SUBMIT_DATA_TO_AN_VIL)) {
        collectTry(errors, () -> checkPiInstitutionRequired(r));
        collectTry(errors, () -> checkNihGrantContractNumberRequired(r));
      }
    }
    collectTry(errors, () -> validateGsrConditionals(r));
    if (Boolean.TRUE.equals(r.getAlternativeDataSharingPlan())) {
      collectTry(errors, () -> checkAltDataSharingExplanationRequired(r));
      collectTry(errors, () -> checkAltDataSharingReasonsRequired(r));
    }
  }

  protected void validateNihAnvilUseConditionals(
      NihAnvilUse anvilUse, StudyRegistrationRequest registration) {
    if (anvilUse.equals(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY)) {
      checkDbGaPPhsIdRequired(registration);
      validatePiInstitutionAndGrantNumber(registration);
    }
    if (anvilUse.equals(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_DO_NOT_HAVE_A_DB_GA_P_PHS_ID)
        || anvilUse.equals(
            NihAnvilUse.I_AM_NOT_NHGRI_FUNDED_BUT_I_AM_SEEKING_TO_SUBMIT_DATA_TO_AN_VIL)) {
      validatePiInstitutionAndGrantNumber(registration);
    }
  }

  protected void validatePiInstitutionAndGrantNumber(StudyRegistrationRequest registration) {
    checkPiInstitutionRequired(registration);
    checkNihGrantContractNumberRequired(registration);
  }

  @VisibleForTesting
  protected void checkDbGaPPhsIdRequired(StudyRegistrationRequest r) {
    if (Objects.isNull(r.getDbGaPPhsID()) || r.getDbGaPPhsID().isBlank()) {
      throw new BadRequestException("dbGaP phs ID is required");
    }
  }

  @VisibleForTesting
  protected void checkPiInstitutionRequired(StudyRegistrationRequest r) {
    if (Objects.isNull(r.getPiInstitution())) {
      throw new BadRequestException("Principal Investigator Institution is required");
    }
  }

  @VisibleForTesting
  protected void checkNihGrantContractNumberRequired(StudyRegistrationRequest r) {
    if (Objects.isNull(r.getNihGrantContractNumber()) || r.getNihGrantContractNumber().isBlank()) {
      throw new BadRequestException("NIH Grant or Contract Number is required");
    }
  }

  protected void validateGsrConditionals(StudyRegistrationRequest registration) {
    String explanation =
        registration.getControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation();
    if (Boolean.TRUE.equals(registration.getControlledAccessRequiredForGenomicSummaryResultsGSR())
        && (Objects.isNull(explanation) || explanation.isBlank())) {
      throw new BadRequestException(
          "Controlled access GSR explanation is required when GSR access is required");
    }
  }

  protected void validateAlternativeDataSharingConditionals(StudyRegistrationRequest registration) {
    if (Boolean.TRUE.equals(registration.getAlternativeDataSharingPlan())) {
      checkAltDataSharingExplanationRequired(registration);
      checkAltDataSharingReasonsRequired(registration);
    }
  }

  @VisibleForTesting
  protected void checkAltDataSharingExplanationRequired(StudyRegistrationRequest r) {
    if (Objects.isNull(r.getAlternativeDataSharingPlanExplanation())) {
      throw new BadRequestException("Alternative Data Sharing Plan Explanation is required");
    }
  }

  @VisibleForTesting
  protected void checkAltDataSharingReasonsRequired(StudyRegistrationRequest r) {
    if (Objects.isNull(r.getAlternativeDataSharingPlanReasons())
        || r.getAlternativeDataSharingPlanReasons().isEmpty()) {
      throw new BadRequestException("Alternative Data Sharing Plan Reasons is required");
    }
  }

  protected void validateEmailFields(StudyRegistrationRequest registration) {
    checkPiEmailValid(registration);
    checkDataCustodianEmailsValid(registration);
  }

  private void collectEmailFieldViolations(StudyRegistrationRequest r, Set<String> errors) {
    collectTry(errors, () -> checkPiEmailValid(r));
    invalidCustodianEmails(r)
        .forEach(
            email -> errors.add("Data Custodian Email is not a valid email address: " + email));
  }

  @VisibleForTesting
  protected void checkPiEmailValid(StudyRegistrationRequest r) {
    EmailValidator emailValidator = EmailValidator.getInstance();
    if (r.getPiEmail() != null
        && !r.getPiEmail().isBlank()
        && !emailValidator.isValid(r.getPiEmail())) {
      throw new BadRequestException("PI Email is not a valid email address");
    }
  }

  @VisibleForTesting
  protected void checkDataCustodianEmailsValid(StudyRegistrationRequest r) {
    List<String> invalid = invalidCustodianEmails(r);
    if (!invalid.isEmpty()) {
      throw new BadRequestException(
          "Data Custodian Email is not a valid email address: " + invalid.getFirst());
    }
  }

  private List<String> invalidCustodianEmails(StudyRegistrationRequest r) {
    if (r.getDataCustodianEmail() == null) {
      return List.of();
    }
    EmailValidator emailValidator = EmailValidator.getInstance();
    return r.getDataCustodianEmail().stream()
        .filter(Objects::nonNull)
        .filter(e -> !e.isBlank())
        .filter(e -> !emailValidator.isValid(e))
        .toList();
  }

  protected void validateDateFields(StudyRegistrationRequest registration) {
    validateDateString(registration.getEmbargoReleaseDate(), "Embargo Release Date");
    validateDateString(
        registration.getAlternativeDataSharingPlanTargetDeliveryDate(),
        "Alternative Data Sharing Plan Target Delivery Date");
    validateDateString(
        registration.getAlternativeDataSharingPlanTargetPublicReleaseDate(),
        "Alternative Data Sharing Plan Target Public Release Date");
  }

  private void collectDateFieldViolations(StudyRegistrationRequest r, Set<String> errors) {
    collectTry(errors, () -> validateDateString(r.getEmbargoReleaseDate(), "Embargo Release Date"));
    collectTry(
        errors,
        () ->
            validateDateString(
                r.getAlternativeDataSharingPlanTargetDeliveryDate(),
                "Alternative Data Sharing Plan Target Delivery Date"));
    collectTry(
        errors,
        () ->
            validateDateString(
                r.getAlternativeDataSharingPlanTargetPublicReleaseDate(),
                "Alternative Data Sharing Plan Target Public Release Date"));
  }

  protected void validateNewConsentGroup(ConsentGroupRequest cg) {
    checkConsentGroupNameRequired(cg);
    checkNumberOfParticipantsRequired(cg);
    validateDataUseConsistency(cg);
    validateDacRequirement(cg);
    validateDateString(cg.getMorDate(), "Moratorium Date");
  }

  @VisibleForTesting
  protected void checkConsentGroupNameRequired(ConsentGroupRequest cg) {
    if (Objects.isNull(cg.getConsentGroupName()) || cg.getConsentGroupName().isBlank()) {
      throw new BadRequestException("Dataset Name is required");
    }
  }

  @VisibleForTesting
  protected void checkNumberOfParticipantsRequired(ConsentGroupRequest cg) {
    if (Objects.isNull(cg.getNumberOfParticipants())) {
      throw new BadRequestException("Number of Participants is required");
    }
  }

  protected void validateDataUseConsistency(ConsentGroupRequest cg) {
    DataUsePrimaryValidator.validate(
        DataUsePrimaryClassifier.classify(
            cg.getGeneralResearchUse(),
            cg.getHmb(),
            cg.getPoa(),
            cg.getDiseaseSpecificUse(),
            cg.getOtherPrimary()),
        cg.getAccessManagement());
  }

  /**
   * A DAC is required unless access management is explicitly {@code open} or {@code external}. A
   * missing accessManagement was treated the same as {@code controlled}.
   */
  protected void validateDacRequirement(ConsentGroupRequest cg) {
    boolean dacNotRequired =
        AccessManagement.OPEN.equals(cg.getAccessManagement())
            || AccessManagement.EXTERNAL.equals(cg.getAccessManagement());
    if (!dacNotRequired && Objects.isNull(cg.getDataAccessCommitteeId())) {
      throw new BadRequestException(
          "Data Access Committee is required unless Access Management is open or external");
    }
  }

  protected void validateDateString(String dateStr, String fieldName) {
    if (dateStr == null || dateStr.isBlank()) {
      return;
    }
    try {
      LocalDate.parse(dateStr);
    } catch (DateTimeParseException e) {
      throw new BadRequestException(fieldName + " is not a valid date", e);
    }
  }
}
