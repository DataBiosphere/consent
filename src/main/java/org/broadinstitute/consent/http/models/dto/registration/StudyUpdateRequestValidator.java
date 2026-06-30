package org.broadinstitute.consent.http.models.dto.registration;

import jakarta.ws.rs.BadRequestException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.collections4.SetUtils;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.service.DatasetService;

public class StudyUpdateRequestValidator extends StudyRegistrationRequestValidator {

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
    if (registration.getStudyName().isBlank()) {
      throw new BadRequestException("Study Name is required");
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
    // null or empty list means "no consent group changes" — removal validation is skipped
    if (registration.getConsentGroups() == null || registration.getConsentGroups().isEmpty()) {
      return;
    }
    Set<Integer> existingDatasetIds = new HashSet<>(existingStudy.getDatasetIds());
    Set<Integer> submittedDatasetIds =
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
}
