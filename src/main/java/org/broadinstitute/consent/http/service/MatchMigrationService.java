package org.broadinstitute.consent.http.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.broadinstitute.consent.http.db.MatchMigrationDAO;
import org.broadinstitute.consent.http.models.matchmigration.MatchMigrationPopulation;
import org.broadinstitute.consent.http.models.matchmigration.MatchMigrationRunReport;
import org.broadinstitute.consent.http.models.matchmigration.MatchMigrationRunResult;
import org.broadinstitute.consent.http.models.matchmigration.SnapshotReconciliation;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.jdbi.v3.core.Jdbi;

/**
 * Rebuilds the match rows that still identify their dataset by legacy text, so they carry a real
 * {@code dataset_id} at the current algorithm version.
 *
 * <p>Reprocesses rather than backfills. The legacy {@code consent} values are consent UUIDs whose
 * dataset mapping was dropped in 2024, so no mapping can be derived without guessing; re-running
 * matching rebuilds each purpose from its DAR instead. A purpose whose DAR carries no dataset
 * associations rebuilds to nothing, which deletes its rows. That is the intended outcome for it,
 * and it is what the application already does whenever a DAR is edited.
 *
 * <p>Snapshots before it touches anything, and is restartable: each purpose is rebuilt in its own
 * transaction, so a failure leaves that purpose's existing matches in place, and the snapshot keeps
 * the first capture rather than overwriting it.
 */
public class MatchMigrationService implements ConsentLogger {

  private final MatchMigrationDAO matchMigrationDAO;
  private final MatchService matchService;

  @Inject
  public MatchMigrationService(Jdbi jdbi, MatchService matchService) {
    this(jdbi.onDemand(MatchMigrationDAO.class), matchService);
  }

  @VisibleForTesting
  MatchMigrationService(MatchMigrationDAO matchMigrationDAO, MatchService matchService) {
    this.matchMigrationDAO = matchMigrationDAO;
    this.matchService = matchService;
  }

  /** Read-only. What a run would do, and whether the constraints would still be refused. */
  public MatchMigrationPopulation findPopulation() {
    return matchMigrationDAO.findPopulation();
  }

  /** Read-only. Reconciles a run that has already happened, for a later confirmation pass. */
  public SnapshotReconciliation reconcile() {
    return matchMigrationDAO.reconcile();
  }

  public MatchMigrationRunResult run() {
    MatchMigrationPopulation before = matchMigrationDAO.findPopulation();
    // Captured before any reprocess, which is what makes the run reversible
    int snapshottedMatches = matchMigrationDAO.snapshotAffectedMatches();
    int snapshottedRationales = matchMigrationDAO.snapshotAffectedRationales();

    List<String> purposes = matchMigrationDAO.findResolvablePurposes();
    List<String> skipped = matchMigrationDAO.findUnresolvablePurposes();
    logInfo(
        ("Reprocessing %d purposes for the match dataset_id migration; "
                + "%d skipped for an archived or missing DAR")
            .formatted(purposes.size(), skipped.size()));

    int reprocessed = 0;
    int retried = 0;
    List<String> failedPurposeIds = new ArrayList<>();
    for (String purposeId : purposes) {
      Outcome outcome = reprocessWithOneRetry(purposeId);
      if (outcome.retried()) {
        retried++;
      }
      if (outcome.failed()) {
        failedPurposeIds.add(purposeId);
      } else {
        reprocessed++;
      }
    }

    MatchMigrationRunReport report =
        new MatchMigrationRunReport(
            snapshottedMatches,
            snapshottedRationales,
            reprocessed,
            skipped.size(),
            failedPurposeIds.size(),
            retried,
            failedPurposeIds,
            skipped);
    MatchMigrationRunResult result =
        new MatchMigrationRunResult(
            before, matchMigrationDAO.findPopulation(), report, matchMigrationDAO.reconcile());
    if (!result.readyForConstraints()) {
      // Said plainly, because the next step is a release whose changeset halts on these conditions
      logWarn("Match dataset_id migration is not yet clear to have its constraints applied");
    }
    return result;
  }

  /**
   * One retry, because a rebuild that failed on a transient database error is worth another attempt
   * and one that failed on a programming error is not worth more than that.
   */
  private Outcome reprocessWithOneRetry(String purposeId) {
    try {
      matchService.reprocessMatchesForPurpose(purposeId);
      return new Outcome(false, false);
    } catch (Exception firstFailure) {
      // The class, never the message: a failure raised while matching can quote a DAR's free text
      logWarn(
          "Match reprocess failed for purpose %s with %s, retrying once"
              .formatted(purposeId, firstFailure.getClass().getName()));
      try {
        matchService.reprocessMatchesForPurpose(purposeId);
        return new Outcome(false, true);
      } catch (Exception retryFailure) {
        logWarn(
            "Match reprocess failed again for purpose %s with %s"
                .formatted(purposeId, retryFailure.getClass().getName()));
        return new Outcome(true, true);
      }
    }
  }

  private record Outcome(boolean failed, boolean retried) {}
}
