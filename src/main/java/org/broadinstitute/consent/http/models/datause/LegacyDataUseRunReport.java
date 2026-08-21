package org.broadinstitute.consent.http.models.datause;

import java.util.List;

/**
 * Outcome of one legacy Data Use run.
 *
 * <p>Redacted: failures carry a dataset id, never an exception message, which can quote the Other
 * free text.
 *
 * @param processed datasets that completed without a failure
 * @param unchanged the subset of those that rebuilt nothing, every DAR having been rebuilt already
 * @param failed datasets whose every attempt failed
 * @param retried datasets that failed once and were attempted again, whether or not that succeeded
 * @param matchesRecomputed DARs rebuilt, counted whether or not their dataset went on to fail
 * @param failedDatasetIds datasets needing follow-up, so a rerun can be scoped to them
 */
public record LegacyDataUseRunReport(
    int processed,
    int unchanged,
    int failed,
    int retried,
    int matchesRecomputed,
    List<Integer> failedDatasetIds) {

  public LegacyDataUseRunReport {
    failedDatasetIds = List.copyOf(failedDatasetIds);
  }
}
