package org.broadinstitute.consent.http.models.datause;

import java.util.Locale;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;

/**
 * One dataset's persisted Data Use as reconciliation reads it: the raw value, so a null can be told
 * from an unparseable one, plus the access management that decides which shape is canonical.
 *
 * @param accessManagement the stored value, or null where the dataset has none
 * @param darCount distinct DARs referencing the dataset
 */
public record PersistedDataUseRow(
    Integer datasetId, String dataUse, String accessManagement, Integer darCount) {

  /** Cross-tabulation label, keeping datasets with no stored value in their own bucket. */
  public String accessManagementLabel() {
    return accessManagement == null || accessManagement.isBlank()
        ? "missing"
        : accessManagement.toLowerCase(Locale.ROOT);
  }

  public boolean isOpenAccess() {
    return AccessManagement.OPEN.value().equalsIgnoreCase(accessManagement);
  }

  public PersistedDataUseClassification classification() {
    return PersistedDataUseClassifier.classify(dataUse);
  }

  public boolean isCanonical() {
    return classification().isCanonicalFor(isOpenAccess());
  }

  /** Has a stored match V5 would now abstain on, and a DAR the recompute can reach it through. */
  public boolean needsMatchRecompute() {
    return classification().abstainsWhenMatched() && darCount != null && darCount > 0;
  }
}
