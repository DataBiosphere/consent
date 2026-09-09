package org.broadinstitute.consent.http.models.matchmigration;

import java.util.List;

/**
 * Outcome of one migration run.
 *
 * <p>Redacted: failures carry a purpose id, never an exception message, which can quote a DAR's
 * free text.
 *
 * @param snapshottedMatches rows captured by this run, zero if an earlier run already held them
 * @param snapshottedRationales rationale rows captured by this run
 * @param reprocessed purposes rebuilt without a failure
 * @param skipped purposes left alone because their DAR is archived or gone
 * @param failed purposes whose every attempt failed
 * @param retried purposes that failed once and were attempted again, successfully or not
 * @param failedPurposeIds purposes needing follow-up, so a rerun can be scoped to them
 * @param skippedPurposeIds purposes the run refused to touch, for the same reason
 */
public record MatchMigrationRunReport(
    int snapshottedMatches,
    int snapshottedRationales,
    int reprocessed,
    int skipped,
    int failed,
    int retried,
    List<String> failedPurposeIds,
    List<String> skippedPurposeIds) {

  public MatchMigrationRunReport {
    failedPurposeIds = List.copyOf(failedPurposeIds);
    skippedPurposeIds = List.copyOf(skippedPurposeIds);
  }
}
