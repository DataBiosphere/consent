package org.broadinstitute.consent.http.models.datause;

/**
 * A run and the classification counts either side of it, so reconciliation is evidence the run
 * produces rather than two calls an operator has to bookend it with.
 */
public record LegacyDataUseRunResult(
    PersistedDataUseReport before, PersistedDataUseReport after, LegacyDataUseRunReport run) {

  /** The run changed no stored Data Use, which is what a recompute-only run must not do. */
  public boolean leftClassificationsUnchanged() {
    return before.countsByClassification().equals(after.countsByClassification());
  }
}
