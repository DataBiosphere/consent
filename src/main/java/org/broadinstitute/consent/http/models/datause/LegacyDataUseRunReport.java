package org.broadinstitute.consent.http.models.datause;

import java.util.List;

/**
 * Outcome of one legacy Data Use run.
 *
 * <p>Redacted: failures carry a dataset id, never an exception message, which can quote the Other
 * free text.
 *
 * @param processed datasets whose matches were recomputed
 * @param failed datasets left unchanged because every attempt failed
 * @param retried datasets that failed once and were attempted again, whether or not that succeeded
 * @param matchesRecomputed DARs whose matches were recomputed
 * @param failedDatasetIds datasets needing follow-up, so a rerun can be scoped to them
 */
public record LegacyDataUseRunReport(
    int processed, int failed, int retried, int matchesRecomputed, List<Integer> failedDatasetIds) {

  public LegacyDataUseRunReport {
    failedDatasetIds = List.copyOf(failedDatasetIds);
  }

  /** Every record accounted for, which is the check a rerun decision rests on. */
  public boolean isComplete(int candidateCount) {
    return processed + failed == candidateCount;
  }
}
