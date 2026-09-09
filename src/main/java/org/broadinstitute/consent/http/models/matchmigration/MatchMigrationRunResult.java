package org.broadinstitute.consent.http.models.matchmigration;

/**
 * A run and the evidence either side of it, so reconciliation is something the run produces rather
 * than two calls an operator has to bookend it with.
 */
public record MatchMigrationRunResult(
    MatchMigrationPopulation before,
    MatchMigrationPopulation after,
    MatchMigrationRunReport run,
    SnapshotReconciliation reconciliation) {

  /**
   * Whether the constraints can now be released: the snapshot accounts for everything and no
   * affected row is left except the ones deliberately skipped.
   */
  public boolean readyForConstraints() {
    return reconciliation.reconciles() && !after.blocksConstraints();
  }
}
