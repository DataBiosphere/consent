package org.broadinstitute.consent.http.models.dataset_registration_v1;

import jakarta.ws.rs.BadRequestException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.NihAnvilUse;

public class DatasetRegistrationSchemaV1UpdateValidator {

  public DatasetRegistrationSchemaV1UpdateValidator() {}

  public boolean validate(Study existingStudy, DatasetRegistrationSchemaV1 registration) {

    // Not modifiable: Study Name, Data Submitter Name/Email, Primary Data Use,
    // Secondary Data Use
    if (Objects.nonNull(registration.getStudyName()) && !registration.getStudyName()
        .equals(existingStudy.getName())) {
      throw new BadRequestException("Invalid change to Study Name");
    }
    if (Objects.nonNull(registration.getDataSubmitterUserId())
        && !registration.getDataSubmitterUserId().equals(existingStudy.getCreateUserId())) {
      throw new BadRequestException("Invalid change to Data Submitter");
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

    // Consent Name changes are not allowed for existing datasets
    List<ConsentGroup> invalidConsentGroupNameChanges = registration.getConsentGroups()
        .stream()
        .filter(cg -> Objects.nonNull(cg.getDatasetId()))
        .filter(cg -> Objects.nonNull(cg.getConsentGroupName()))
        .toList();
    if (!invalidConsentGroupNameChanges.isEmpty()) {
      throw new BadRequestException("Invalid Data Use Name changes to existing Consent Groups");
    }

    // Dac IDs are required for all consent groups
    List<ConsentGroup> invalidDACConsentGroup = registration.getConsentGroups()
        .stream()
        .filter(cg -> Objects.isNull(cg.getDataAccessCommitteeId()))
        .toList();
    if (!invalidDACConsentGroup.isEmpty()) {
      throw new BadRequestException("Missing DAC Selection for Consent Groups");
    }

    // Data Use required for all new consent groups
    List<ConsentGroup> missingDataUseConsentGroups = registration.getConsentGroups()
        .stream()
        .filter(cg -> Objects.isNull(cg.getDatasetId()))
        .filter(cg -> !cg.hasPrimaryDataUse())
        .toList();
    if (!missingDataUseConsentGroups.isEmpty()) {
      throw new BadRequestException("Missing Data Use Selection for Consent Groups");
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
    HashSet<Integer> existingDatasetIds = new HashSet<>(existingStudy.getDatasetIds());
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
        anvilUse.equals(NihAnvilUse.I_AM_NOT_NHGRI_FUNDED_BUT_I_AM_SEEKING_TO_SUBMIT_DATA_TO_AN_VIL)) {
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
    if (Objects.isNull(registration.getDataCustodianEmail()) || registration.getDataCustodianEmail().isEmpty()) {
      throw new BadRequestException("Data Custodian Email is required");
    }

    return true;
  }

}
