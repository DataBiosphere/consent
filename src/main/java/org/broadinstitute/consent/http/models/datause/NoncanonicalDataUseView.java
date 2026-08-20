package org.broadinstitute.consent.http.models.datause;

/**
 * A dataset the canonical validator would reject, identified so an admin can act on it. Carries the
 * classification label, not the stored value, so no Other free text is exposed.
 *
 * @param needsMatchRecompute whether a DAR reaches it, and so whether the recompute covers it
 */
public record NoncanonicalDataUseView(
    Integer datasetId,
    String classification,
    String accessManagement,
    Integer darCount,
    boolean needsMatchRecompute) {

  public static NoncanonicalDataUseView from(PersistedDataUseRow row) {
    return new NoncanonicalDataUseView(
        row.datasetId(),
        row.classification().label(),
        row.accessManagementLabel(),
        row.darCount() == null ? 0 : row.darCount(),
        row.needsMatchRecompute());
  }
}
