package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.broadinstitute.consent.http.db.PersistedDataUseDAO;
import org.broadinstitute.consent.http.models.datause.LegacyDataUseRunReport;
import org.broadinstitute.consent.http.models.datause.LegacyDataUseRunResult;
import org.broadinstitute.consent.http.models.datause.NoncanonicalDataUseView;
import org.broadinstitute.consent.http.models.datause.PersistedDataUseReport;
import org.broadinstitute.consent.http.models.datause.PersistedDataUseRow;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.jdbi.v3.core.Jdbi;

/**
 * Recomputes the matches the legacy Data Use population affects, and reports on its shapes.
 *
 * <p>Writes no stored Data Use, so a rerun is safe by construction rather than by checkpoint.
 * Matches are recomputed per DAR, which never touches elections or votes. Correcting a noncanonical
 * record is a domain decision, applied through {@code PUT /api/dataset/{id}/datause}.
 */
public class LegacyDataUseService implements ConsentLogger {

  private final PersistedDataUseDAO persistedDataUseDAO;
  private final MatchService matchService;

  @Inject
  public LegacyDataUseService(Jdbi jdbi, MatchService matchService) {
    this(jdbi.onDemand(PersistedDataUseDAO.class), matchService);
  }

  LegacyDataUseService(PersistedDataUseDAO persistedDataUseDAO, MatchService matchService) {
    this.persistedDataUseDAO = persistedDataUseDAO;
    this.matchService = matchService;
  }

  private PersistedDataUseReport report() {
    return PersistedDataUseReport.from(persistedDataUseDAO.findAllPersistedDataUse());
  }

  /** Datasets whose shape the canonical validator would now reject. */
  public List<PersistedDataUseRow> findNoncanonicalRows() {
    return persistedDataUseDAO.findAllPersistedDataUse().stream()
        .filter(row -> !row.isCanonical())
        .toList();
  }

  /** Identified so an admin can act on them; classification labels only. */
  public List<NoncanonicalDataUseView> findNoncanonicalViews() {
    return findNoncanonicalRows().stream().map(NoncanonicalDataUseView::from).toList();
  }

  /** Abstaining datasets reachable through a DAR, which is the unit recompute works by. */
  public List<PersistedDataUseRow> findRowsNeedingMatchRecompute() {
    return persistedDataUseDAO.findAllPersistedDataUse().stream()
        .filter(PersistedDataUseRow::needsMatchRecompute)
        .toList();
  }

  /** Recomputes only what a DAR can reach, so an abstaining row without one is left alone. */
  public LegacyDataUseRunResult recomputeAbstainingMatches() {
    PersistedDataUseReport before = report();
    List<PersistedDataUseRow> abstaining =
        persistedDataUseDAO.findAllPersistedDataUse().stream()
            .filter(row -> row.classification().abstainsWhenMatched())
            .toList();
    List<PersistedDataUseRow> candidates =
        abstaining.stream().filter(PersistedDataUseRow::needsMatchRecompute).toList();
    // Counted in the log so unreachable rows are not mistaken for done
    logInfo(
        "Recomputing matches for %d abstaining datasets; %d have no DAR relation and are left alone"
            .formatted(candidates.size(), abstaining.size() - candidates.size()));
    return new LegacyDataUseRunResult(before, report(), run(candidates));
  }

  LegacyDataUseRunReport run(List<PersistedDataUseRow> candidates) {
    int processed = 0;
    int retried = 0;
    int matchesRecomputed = 0;
    List<Integer> failedDatasetIds = new ArrayList<>();

    for (PersistedDataUseRow candidate : candidates) {
      Outcome outcome = recomputeWithOneRetry(candidate.datasetId());
      if (outcome.retried()) {
        retried++;
      }
      if (outcome.failed()) {
        failedDatasetIds.add(candidate.datasetId());
      } else {
        processed++;
        matchesRecomputed += outcome.matchesRecomputed();
      }
    }

    return new LegacyDataUseRunReport(
        processed, failedDatasetIds.size(), retried, matchesRecomputed, failedDatasetIds);
  }

  private Outcome recomputeWithOneRetry(Integer datasetId) {
    try {
      return new Outcome(false, recomputeMatches(datasetId), false);
    } catch (Exception _) {
      // No message or cause: a failure raised while matching can quote the Other free text
      logWarn("Legacy match recompute failed for dataset %d, retrying once".formatted(datasetId));
      try {
        return new Outcome(false, recomputeMatches(datasetId), true);
      } catch (Exception _) {
        logWarn("Legacy match recompute failed again for dataset %d".formatted(datasetId));
        return new Outcome(true, 0, true);
      }
    }
  }

  private int recomputeMatches(Integer datasetId) {
    List<String> referenceIds = persistedDataUseDAO.findDarReferenceIdsByDatasetId(datasetId);
    referenceIds.forEach(matchService::reprocessMatchesForPurpose);
    return referenceIds.size();
  }

  private record Outcome(boolean failed, int matchesRecomputed, boolean retried) {}
}
