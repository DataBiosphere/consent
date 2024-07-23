package org.broadinstitute.consent.http.models.dataset_registration_v1;

import jakarta.ws.rs.BadRequestException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.collections4.SetUtils;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.NihAnvilUse;
import org.broadinstitute.consent.http.service.DatasetService;

public class DatasetRegistrationSchemaV1UpdateValidator {

  private final DatasetService datasetService;

  public DatasetRegistrationSchemaV1UpdateValidator(DatasetService datasetService) {
    this.datasetService = datasetService;
  }

  public boolean validate(Study existingStudy, DatasetRegistrationSchemaV1 registration) {

    // Validate that the new study name is unique
    Set<String> studyNames = datasetService.findAllStudyNames();
    if (studyNames.contains(registration.getStudyName()) &&
        !registration.getStudyName().equals(existingStudy.getName())) {
      throw new BadRequestException("Invalid change to Study Name");
    }

    // Not modifiable: Data Submitter Name/Email, Primary Data Use,
    // Secondary Data Use
    if (registration.getDataSubmitterUserId() != null
        && !registration.getDataSubmitterUserId().equals(existingStudy.getCreateUserId())) {
      throw new BadRequestException("Invalid change to Data Submitter");
    }

    // Minimum number of consent groups is 1
    if (registration.getConsentGroups().isEmpty()) {
      throw new BadRequestException("Invalid number of Consent Groups");
    }

    // Data use changes are not allowed for existing datasets
    List<ConsentGroup> invalidConsentGroups = registration.getConsentGroups()
        .stream()
        .filter(cg -> Objects.nonNull(cg.getDatasetId()))
        .filter(ConsentGroup::isInvalidForUpdate)
        .toList();
    if (!invalidConsentGroups.isEmpty()) {
      throw new BadRequestException("Invalid Data Use changes to existing Consent Groups");
    }

    // Ensure that all consent group changes are for datasets in the current study
    List<ConsentGroup> nonStudyConsentGroups = registration.getConsentGroups()
        .stream()
        .filter(cg -> Objects.nonNull(cg.getDatasetId()))
        .filter(cg -> existingStudy
            .getDatasetIds()
            .stream()
            .noneMatch(id -> id.equals(cg.getDatasetId())))
        .toList();
    if (!nonStudyConsentGroups.isEmpty()) {
      throw new BadRequestException("Invalid Consent Group changes to study");
    }

    // Consent Name changes are not allowed for existing datasets unless it is empty
    List<ConsentGroup> invalidConsentGroupNameChanges = registration.getConsentGroups()
        .stream()
        .filter(cg -> Objects.nonNull(cg.getDatasetId()))
        .filter(cg -> Objects.nonNull(cg.getConsentGroupName()))
        // If the dataset already has a name, this consent group is invalid
        .filter(cg -> {
          Optional<Dataset> dataset = SetUtils.emptyIfNull(existingStudy.getDatasets())
              .stream()
              .filter(d -> d.getDataSetId().equals(cg.getDatasetId()))
              .findFirst();
          return dataset.isPresent() && !dataset.get().getName().isBlank();
        })
        .toList();
    if (!invalidConsentGroupNameChanges.isEmpty()) {
      throw new BadRequestException("Invalid Name changes to existing Consent Groups");
    }

    // Data Location required for all consent groups
    List<ConsentGroup> missingDataLocationConsentGroups = registration.getConsentGroups()
        .stream()
        .filter(cg -> Objects.isNull(cg.getDataLocation()))
        .toList();
    if (!missingDataLocationConsentGroups.isEmpty()) {
      throw new BadRequestException("Missing Data Location for Consent Groups");
    }

    // Validate that we're not trying to delete any datasets in the registration payload.
    // The list of non-null dataset ids in the consent groups MUST be the same as the list of
    // existing dataset ids.
    HashSet<Integer> existingDatasetIds =
        Objects.nonNull(existingStudy.getDatasetIds()) ? new HashSet<>(
            existingStudy.getDatasetIds()) : new HashSet<>();
    HashSet<Integer> consentGroupDatasetIds = new HashSet<>(registration.getConsentGroups()
        .stream()
        .map(ConsentGroup::getDatasetId)
        .filter(Objects::nonNull)
        .toList());
    if (!consentGroupDatasetIds.containsAll(existingDatasetIds)) {
      throw new BadRequestException("Invalid removal of Consent Groups");
    }

    // Ensure that required values exist:
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
    if (anvilUse.equals(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_DO_NOT_HAVE_A_DB_GA_P_PHS_ID) ||
        anvilUse.equals(
            NihAnvilUse.I_AM_NOT_NHGRI_FUNDED_BUT_I_AM_SEEKING_TO_SUBMIT_DATA_TO_AN_VIL)) {
      if (Objects.isNull(registration.getPiInstitution())) {
        throw new BadRequestException("PI Institution is required");
      }
      if (Objects.isNull(registration.getNihGrantContractNumber())) {
        throw new BadRequestException("NIH Grant of Contract Number is required");
      }
    }
    if (Objects.isNull(registration.getPhenotypeIndication())) {
      throw new BadRequestException("Phenotype Indication is required");
    }
    if (Objects.isNull(registration.getPiName())) {
      throw new BadRequestException("Principal Investigator is required");
    }
    if (Objects.isNull(registration.getDataCustodianEmail()) || registration.getDataCustodianEmail()
        .isEmpty()) {
      throw new BadRequestException("Data Custodian Email is required");
    }

    return true;
  }

}
