package org.broadinstitute.consent.http.models.matchmigration;

/**
 * The snapshot measured against what the run left behind.
 *
 * <p>A reprocess deletes a purpose's rows and inserts replacements, which take new ids. So a
 * snapshotted row that is gone was handled, whether it was rebuilt under a new id or deliberately
 * removed because its DAR carries no dataset associations. A snapshotted row that is still there
 * was not handled, and after a complete run the only such rows are the ones the run skipped.
 *
 * @param snapshotted rows captured before the run
 * @param snapshottedRowsGone captured rows the run replaced or removed
 * @param snapshottedRowsRemaining captured rows still present, expected to be the skipped ones
 * @param unresolvableRows affected rows on purposes whose DAR does not resolve, skipped by design
 * @param snapshottedPurposes distinct purposes captured
 * @param purposesHoldingMatches captured purposes now holding at least one match. Named for the
 *     state rather than the cause: a skipped purpose still holds its original rows, so it counts
 *     here without having been rebuilt
 * @param purposesHoldingNoMatches captured purposes now holding none - the intended deletions,
 *     where the DAR carries no dataset associations to rebuild from
 * @param stillAffected rows the constraints would still reject
 */
public record SnapshotReconciliation(
    int snapshotted,
    int snapshottedRowsGone,
    int snapshottedRowsRemaining,
    int unresolvableRows,
    int snapshottedPurposes,
    int purposesHoldingMatches,
    int purposesHoldingNoMatches,
    int stillAffected) {

  /**
   * Every captured row was either handled or deliberately skipped, and nothing else is outstanding.
   * Stated as two equalities rather than one so a run that skipped more than it meant to fails here
   * instead of looking complete.
   */
  public boolean reconciles() {
    return snapshottedRowsRemaining == unresolvableRows && stillAffected == unresolvableRows;
  }
}
