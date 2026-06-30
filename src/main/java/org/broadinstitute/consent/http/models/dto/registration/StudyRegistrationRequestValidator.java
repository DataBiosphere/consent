package org.broadinstitute.consent.http.models.dto.registration;

import jakarta.ws.rs.BadRequestException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import org.apache.commons.validator.routines.EmailValidator;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.NihAnvilUse;

public class StudyRegistrationRequestValidator {

  public boolean validate(StudyRegistrationRequest registration) {
    validateRequiredStudyFields(registration);
    validateConditionalFields(registration);
    validateEmailFields(registration);
    validateDateFields(registration);
    validateConsentGroups(registration.getConsentGroups());
    return true;
  }

  private void validateRequiredStudyFields(StudyRegistrationRequest registration) {
    if (Objects.isNull(registration.getStudyName()) || registration.getStudyName().isBlank()) {
      throw new BadRequestException("Study Name is required");
    }
    if (Objects.isNull(registration.getStudyDescription())) {
      throw new BadRequestException("Study Description is required");
    }
    if (Objects.isNull(registration.getDataTypes()) || registration.getDataTypes().isEmpty()) {
      throw new BadRequestException("Data Types is required");
    }
    if (Objects.isNull(registration.getPublicVisibility())) {
      throw new BadRequestException("Public Visibility is required");
    }
    if (Objects.isNull(registration.getNihAnvilUse())) {
      throw new BadRequestException("NIH Anvil Use is required");
    }
    if (Objects.isNull(registration.getPiName())) {
      throw new BadRequestException("Principal Investigator is required");
    }
  }

  protected void validateConditionalFields(StudyRegistrationRequest registration) {
    validateNihAnvilUseConditionals(registration.getNihAnvilUse(), registration);
    validateGsrConditionals(registration);
    validateAlternativeDataSharingConditionals(registration);
  }

  protected void validateNihAnvilUseConditionals(
      NihAnvilUse anvilUse, StudyRegistrationRequest registration) {
    if (anvilUse.equals(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY)) {
      if (Objects.isNull(registration.getDbGaPPhsID())) {
        throw new BadRequestException("dbGaPPhsID is required");
      }
      validatePiInstitutionAndGrantNumber(registration);
    }
    if (anvilUse.equals(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_DO_NOT_HAVE_A_DB_GA_P_PHS_ID)
        || anvilUse.equals(
            NihAnvilUse.I_AM_NOT_NHGRI_FUNDED_BUT_I_AM_SEEKING_TO_SUBMIT_DATA_TO_AN_VIL)) {
      validatePiInstitutionAndGrantNumber(registration);
    }
  }

  protected void validatePiInstitutionAndGrantNumber(StudyRegistrationRequest registration) {
    if (Objects.isNull(registration.getPiInstitution())) {
      throw new BadRequestException("PI Institution is required");
    }
    if (Objects.isNull(registration.getNihGrantContractNumber())) {
      throw new BadRequestException("NIH Grant or Contract Number is required");
    }
  }

  protected void validateGsrConditionals(StudyRegistrationRequest registration) {
    if (Boolean.TRUE.equals(registration.getControlledAccessRequiredForGenomicSummaryResultsGSR())
        && Objects.isNull(
            registration
                .getControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation())) {
      throw new BadRequestException(
          "Controlled access GSR explanation is required when GSR access is required");
    }
  }

  protected void validateAlternativeDataSharingConditionals(StudyRegistrationRequest registration) {
    if (Boolean.TRUE.equals(registration.getAlternativeDataSharingPlan())) {
      if (Objects.isNull(registration.getAlternativeDataSharingPlanExplanation())) {
        throw new BadRequestException("Alternative Data Sharing Plan Explanation is required");
      }
      if (Objects.isNull(registration.getAlternativeDataSharingPlanReasons())
          || registration.getAlternativeDataSharingPlanReasons().isEmpty()) {
        throw new BadRequestException("Alternative Data Sharing Plan Reasons is required");
      }
    }
  }

  protected void validateEmailFields(StudyRegistrationRequest registration) {
    EmailValidator emailValidator = EmailValidator.getInstance();
    if (registration.getPiEmail() != null
        && !registration.getPiEmail().isBlank()
        && !emailValidator.isValid(registration.getPiEmail())) {
      throw new BadRequestException("PI Email is not a valid email address");
    }
    if (registration.getDataCustodianEmail() != null) {
      registration.getDataCustodianEmail().stream()
          .filter(Objects::nonNull)
          .filter(e -> !e.isBlank())
          .forEach(
              email -> {
                if (!emailValidator.isValid(email)) {
                  throw new BadRequestException(
                      "Data Custodian Email is not a valid email address: " + email);
                }
              });
    }
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

  private void validateConsentGroups(List<ConsentGroupRequest> consentGroups) {
    if (Objects.isNull(consentGroups) || consentGroups.isEmpty()) {
      throw new BadRequestException("At least one Consent Group is required");
    }
    consentGroups.forEach(this::validateNewConsentGroup);
  }

  protected void validateNewConsentGroup(ConsentGroupRequest cg) {
    if (Objects.isNull(cg.getConsentGroupName()) || cg.getConsentGroupName().isBlank()) {
      throw new BadRequestException("Consent Group Name is required");
    }
    if (Objects.isNull(cg.getNumberOfParticipants())) {
      throw new BadRequestException("Number of Participants is required");
    }
    validateDataUseConsistency(cg);
    validateDacRequirement(cg);
    validateDateString(cg.getMorDate(), "Moratorium Date");
  }

  protected void validateDataUseConsistency(ConsentGroupRequest cg) {
    int count = 0;
    if (AccessManagement.OPEN.equals(cg.getAccessManagement())) count++;
    if (Boolean.TRUE.equals(cg.getGeneralResearchUse())) count++;
    if (Boolean.TRUE.equals(cg.getHmb())) count++;
    if (Boolean.TRUE.equals(cg.getPoa())) count++;
    if (cg.getDiseaseSpecificUse() != null && !cg.getDiseaseSpecificUse().isEmpty()) count++;
    if (cg.getOtherPrimary() != null && !cg.getOtherPrimary().isBlank()) count++;
    if (count != 1) {
      throw new BadRequestException(
          "Consent Group must have exactly one primary data use (open access, or one of:"
              + " general research use, health/medical/biomedical, populations/origins/ancestry,"
              + " disease-specific, other)");
    }
  }

  protected void validateDacRequirement(ConsentGroupRequest cg) {
    if (AccessManagement.CONTROLLED.equals(cg.getAccessManagement())
        && Objects.isNull(cg.getDataAccessCommitteeId())) {
      throw new BadRequestException(
          "Data Access Committee is required for controlled access datasets");
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
