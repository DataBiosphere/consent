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
 * @param reprocessed purposes rebuilt successfully, counting any that succeeded on a retry. With
 *     {@code failed} this accounts for every purpose attempted; {@code retried} overlaps both
 *     rather than forming a bucket of its own
 * @param skipped purposes left alone because their DAR is archived or gone
 * @param failed purposes whose every attempt failed
 * @param retried purposes that failed once and were attempted again, successfully or not
 * @param failedPurposeIds purposes needing follow-up. Investigation only: a rerun takes no
 *     arguments and re-attempts every purpose still affected
 * @param skippedPurposeIds purposes the run refused to touch, on the same terms
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
