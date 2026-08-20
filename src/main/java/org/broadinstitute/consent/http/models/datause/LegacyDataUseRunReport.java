package org.broadinstitute.consent.http.models.datause;

import java.util.List;
import java.util.Map;

/**
 * Outcome of one legacy Data Use run.
 *
 * <p>Redacted: failures carry a dataset id and reason category, never an exception message, which
 * can quote the Other free text.
 *
 * @param processed records changed, whether normalized or match-recomputed
 * @param skipped records already canonical, or deferred to the curator-assisted process
 * @param failed records left unchanged because every attempt failed
 * @param retried records that failed once and were attempted again, whether or not that succeeded
 * @param matchesRecomputed DARs whose matches were recomputed
 * @param failuresByReason failure count per reason category
 * @param failedDatasetIds datasets needing follow-up, so a rerun can be scoped to them
 */
public record LegacyDataUseRunReport(
    int processed,
    int skipped,
    int failed,
    int retried,
    int matchesRecomputed,
    Map<String, Integer> failuresByReason,
    List<Integer> failedDatasetIds) {

  public LegacyDataUseRunReport {
    failuresByReason = Map.copyOf(failuresByReason);
    failedDatasetIds = List.copyOf(failedDatasetIds);
  }

  /** Every record accounted for, which is the check a rerun decision rests on. */
  public boolean isComplete(int candidateCount) {
    return processed + skipped + failed == candidateCount;
  }
}
