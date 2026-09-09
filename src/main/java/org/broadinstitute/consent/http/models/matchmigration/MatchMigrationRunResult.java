package org.broadinstitute.consent.http.models.matchmigration;

/**
 * A run and the evidence either side of it, so reconciliation is something the run produces rather
 * than two calls an operator has to bookend it with.
 */
public record MatchMigrationRunResult(
    MatchMigrationPopulation before,
    MatchMigrationPopulation after,
    MatchMigrationRunReport run,
    SnapshotReconciliation reconciliation,
    boolean readyForConstraints) {

  /**
   * Whether the constraints can now be released: the snapshot accounts for everything and no
   * affected row is left except the ones deliberately skipped.
   *
   * <p>Computed here into a component rather than exposed as a method, because responses serialize
   * with Gson, which reads fields and drops computed accessors. This is the run's headline signal,
   * so it has to reach the operator reading the response.
   */
  public static MatchMigrationRunResult of(
      MatchMigrationPopulation before,
      MatchMigrationPopulation after,
      MatchMigrationRunReport run,
      SnapshotReconciliation reconciliation) {
    return new MatchMigrationRunResult(
        before,
        after,
        run,
        reconciliation,
        reconciliation.reconciles() && !after.blocksConstraints());
  }
}
