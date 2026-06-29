package org.broadinstitute.consent.http.models.dto.registration;

import jakarta.ws.rs.BadRequestException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.NihAnvilUse;
import org.broadinstitute.consent.http.service.DatasetService;

public class StudyUpdateRequestValidator {

  private final DatasetService datasetService;

  public StudyUpdateRequestValidator(DatasetService datasetService) {
    this.datasetService = datasetService;
  }

  /**
   * Validates a study update request against the existing study state.
   *
   * <p>Note: dataSubmitterUserId is excluded from StudyUpdateRequest by design and cannot be
   * changed through this endpoint; submitter immutability is enforced structurally.
   */
  public boolean validate(Study existingStudy, StudyUpdateRequest registration) {
    validateStudyNameUniqueness(existingStudy, registration);
    validateRequiredFields(registration);
    validateConditionalFields(registration);
    validateEmailFields(registration);
    validateDateFields(registration);
    validateConsentGroupMembership(existingStudy, registration);
    validateConsentGroupNameChanges(existingStudy, registration);
    validateConsentGroupRemoval(existingStudy, registration);
    validateNewConsentGroups(registration.getConsentGroups());
    return true;
  }

  private void validateStudyNameUniqueness(Study existingStudy, StudyUpdateRequest registration) {
    if (registration.getStudyName() == null) {
      return;
    }
    if (registration.getStudyName().equals(existingStudy.getName())) {
      return;
    }
    Set<String> studyNames = datasetService.findAllStudyNames();
    if (studyNames.contains(registration.getStudyName())) {
      throw new BadRequestException("Invalid change to Study Name");
    }
  }

  private void validateRequiredFields(StudyUpdateRequest registration) {
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

  private void validateConditionalFields(StudyUpdateRequest registration) {
    NihAnvilUse anvilUse = registration.getNihAnvilUse();
    if (anvilUse.equals(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY)) {
      if (Objects.isNull(registration.getDbGaPPhsID())) {
        throw new BadRequestException("DbGap phs ID is required");
      }
      if (Objects.isNull(registration.getPiInstitution())) {
        throw new BadRequestException("PI Institution is required");
      }
      if (Objects.isNull(registration.getNihGrantContractNumber())) {
        throw new BadRequestException("NIH Grant of Contract Number is required");
      }
    }
    if (anvilUse.equals(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_DO_NOT_HAVE_A_DB_GA_P_PHS_ID)
        || anvilUse.equals(
            NihAnvilUse.I_AM_NOT_NHGRI_FUNDED_BUT_I_AM_SEEKING_TO_SUBMIT_DATA_TO_AN_VIL)) {
      if (Objects.isNull(registration.getPiInstitution())) {
        throw new BadRequestException("PI Institution is required");
      }
      if (Objects.isNull(registration.getNihGrantContractNumber())) {
        throw new BadRequestException("NIH Grant of Contract Number is required");
      }
    }
    if (Boolean.TRUE.equals(
        registration.getControlledAccessRequiredForGenomicSummaryResultsGSR())) {
      if (Objects.isNull(
          registration
              .getControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation())) {
        throw new BadRequestException(
            "Controlled access GSR explanation is required when GSR access is required");
      }
    }
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

  private void validateEmailFields(StudyUpdateRequest registration) {
    EmailValidator emailValidator = EmailValidator.getInstance();
    if (registration.getPiEmail() != null && !registration.getPiEmail().isBlank()) {
      if (!emailValidator.isValid(registration.getPiEmail())) {
        throw new BadRequestException("PI Email is not a valid email address");
      }
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

  private void validateDateFields(StudyUpdateRequest registration) {
    validateDateString(registration.getEmbargoReleaseDate(), "Embargo Release Date");
    validateDateString(
        registration.getAlternativeDataSharingPlanTargetDeliveryDate(),
        "Alternative Data Sharing Plan Target Delivery Date");
    validateDateString(
        registration.getAlternativeDataSharingPlanTargetPublicReleaseDate(),
        "Alternative Data Sharing Plan Target Public Release Date");
  }

  private void validateConsentGroupMembership(
      Study existingStudy, StudyUpdateRequest registration) {
    if (registration.getConsentGroups() == null) {
      return;
    }
    List<ConsentGroupRequest> nonStudyGroups =
        registration.getConsentGroups().stream()
            .filter(cg -> Objects.nonNull(cg.getDatasetId()))
            .filter(
                cg ->
                    existingStudy.getDatasetIds().stream()
                        .noneMatch(id -> id.equals(cg.getDatasetId())))
            .toList();
    if (!nonStudyGroups.isEmpty()) {
      throw new BadRequestException("Invalid Consent Group changes to study");
    }
  }

  private void validateConsentGroupNameChanges(
      Study existingStudy, StudyUpdateRequest registration) {
    if (registration.getConsentGroups() == null) {
      return;
    }
    List<ConsentGroupRequest> invalidNameChanges =
        registration.getConsentGroups().stream()
            .filter(cg -> Objects.nonNull(cg.getDatasetId()))
            .filter(cg -> Objects.nonNull(cg.getConsentGroupName()))
            .filter(
                cg -> {
                  Optional<Dataset> dataset =
                      SetUtils.emptyIfNull(existingStudy.getDatasets()).stream()
                          .filter(d -> d.getDatasetId().equals(cg.getDatasetId()))
                          .findFirst();
                  if (dataset.isEmpty()) {
                    return false;
                  }
                  String storedName = dataset.get().getName();
                  // Only block a rename: stored name is set AND the submitted name differs
                  return storedName != null
                      && !storedName.isBlank()
                      && !cg.getConsentGroupName().equals(storedName);
                })
            .toList();
    if (!invalidNameChanges.isEmpty()) {
      throw new BadRequestException("Invalid Name changes to existing Consent Groups");
    }
  }

  private void validateConsentGroupRemoval(Study existingStudy, StudyUpdateRequest registration) {
    if (registration.getConsentGroups() == null || registration.getConsentGroups().isEmpty()) {
      return;
    }
    HashSet<Integer> existingDatasetIds = new HashSet<>(existingStudy.getDatasetIds());
    HashSet<Integer> submittedDatasetIds =
        new HashSet<>(
            registration.getConsentGroups().stream()
                .map(ConsentGroupRequest::getDatasetId)
                .filter(Objects::nonNull)
                .toList());
    if (!submittedDatasetIds.containsAll(existingDatasetIds)) {
      throw new BadRequestException("Invalid removal of Consent Groups");
    }
  }

  private void validateNewConsentGroups(List<ConsentGroupRequest> consentGroups) {
    if (consentGroups == null) {
      return;
    }
    consentGroups.stream()
        .filter(cg -> Objects.isNull(cg.getDatasetId()))
        .forEach(this::validateNewConsentGroup);
  }

  private void validateNewConsentGroup(ConsentGroupRequest cg) {
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

  private void validateDataUseConsistency(ConsentGroupRequest cg) {
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

  private void validateDacRequirement(ConsentGroupRequest cg) {
    if (AccessManagement.CONTROLLED.equals(cg.getAccessManagement())
        && Objects.isNull(cg.getDataAccessCommitteeId())) {
      throw new BadRequestException(
          "Data Access Committee is required for controlled access datasets");
    }
  }

  private void validateDateString(String dateStr, String fieldName) {
    if (dateStr == null || dateStr.isBlank()) {
      return;
    }
    try {
      LocalDate.parse(dateStr);
    } catch (DateTimeParseException e) {
      throw new BadRequestException(fieldName + " is not a valid date");
    }
  }
}
