package org.broadinstitute.consent.http.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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

  @VisibleForTesting
  LegacyDataUseService(PersistedDataUseDAO persistedDataUseDAO, MatchService matchService) {
    this.persistedDataUseDAO = persistedDataUseDAO;
    this.matchService = matchService;
  }

  private PersistedDataUseReport report() {
    return PersistedDataUseReport.from(persistedDataUseDAO.findAllPersistedDataUse());
  }

  /**
   * Identified so an admin can act on them; classification labels only. The rows themselves stay
   * inside the service, since they carry the raw value and it can hold Other free text.
   */
  public List<NoncanonicalDataUseView> findNoncanonicalViews() {
    return persistedDataUseDAO.findAllPersistedDataUse().stream()
        .filter(row -> !row.isCanonical())
        .map(NoncanonicalDataUseView::from)
        .toList();
  }

  /** Recomputes only what a DAR can reach, so an abstaining row without one is left alone. */
  public LegacyDataUseRunResult recomputeAbstainingMatches() {
    List<PersistedDataUseRow> rows = persistedDataUseDAO.findAllPersistedDataUse();
    PersistedDataUseReport before = PersistedDataUseReport.from(rows);
    // Partitioned from one parse per row, and by the same predicate the run works by
    Map<Boolean, List<PersistedDataUseRow>> abstaining =
        rows.stream()
            .map(row -> Map.entry(row, row.classification()))
            .filter(entry -> entry.getValue().abstainsWhenMatched())
            .collect(
                Collectors.partitioningBy(
                    entry -> entry.getKey().needsMatchRecompute(entry.getValue()),
                    Collectors.mapping(Map.Entry::getKey, Collectors.toList())));
    List<PersistedDataUseRow> candidates = abstaining.get(true);
    // Counted in the log so unreachable rows are not mistaken for done
    logInfo(
        "Recomputing matches for %d abstaining datasets; %d have no DAR relation and are left alone"
            .formatted(candidates.size(), abstaining.get(false).size()));
    LegacyDataUseRunReport run = run(candidates);
    LegacyDataUseRunResult result = new LegacyDataUseRunResult(before, report(), run);
    if (!result.leftClassificationsUnchanged()) {
      // A recompute writes no stored Data Use, so a change here came from outside this run
      logWarn("Legacy Data Use classifications changed during a recompute-only run");
    }
    return result;
  }

  LegacyDataUseRunReport run(List<PersistedDataUseRow> candidates) {
    int processed = 0;
    int unchanged = 0;
    int retried = 0;
    int matchesRecomputed = 0;
    List<Integer> failedDatasetIds = new ArrayList<>();
    // Reprocessing a DAR covers every dataset on it, so candidates sharing one rebuild it once
    Set<String> recomputed = new HashSet<>();

    for (PersistedDataUseRow candidate : candidates) {
      Outcome outcome = recomputeWithOneRetry(candidate.datasetId(), recomputed);
      if (outcome.retried()) {
        retried++;
      }
      // Counted either way: a dataset that failed on its last DAR still rebuilt the earlier ones
      matchesRecomputed += outcome.matchesRecomputed();
      if (outcome.failed()) {
        failedDatasetIds.add(candidate.datasetId());
      } else {
        processed++;
        if (outcome.matchesRecomputed() == 0) {
          unchanged++;
        }
      }
    }

    return new LegacyDataUseRunReport(
        processed,
        unchanged,
        failedDatasetIds.size(),
        retried,
        matchesRecomputed,
        failedDatasetIds);
  }

  /**
   * Counts by how much the run's rebuilt set grew rather than by what an attempt returned, so a
   * retry that skips the DARs the first attempt already rebuilt still credits them.
   */
  private Outcome recomputeWithOneRetry(Integer datasetId, Set<String> recomputed) {
    int before = recomputed.size();
    try {
      recomputeMatches(datasetId, recomputed);
      return new Outcome(false, recomputed.size() - before, false);
    } catch (Exception firstFailure) {
      // The class, never the message: a failure raised while matching can quote the Other free
      // text. Naming it keeps a programming error from reading as infrastructure worth retrying.
      logWarn(
          "Legacy match recompute failed for dataset %d with %s, retrying once"
              .formatted(datasetId, firstFailure.getClass().getName()));
      try {
        recomputeMatches(datasetId, recomputed);
        return new Outcome(false, recomputed.size() - before, true);
      } catch (Exception retryFailure) {
        logWarn(
            "Legacy match recompute failed again for dataset %d with %s"
                .formatted(datasetId, retryFailure.getClass().getName()));
        return new Outcome(true, recomputed.size() - before, true);
      }
    }
  }

  private void recomputeMatches(Integer datasetId, Set<String> recomputed) {
    for (String referenceId : persistedDataUseDAO.findDarReferenceIdsByDatasetId(datasetId)) {
      if (recomputed.contains(referenceId)) {
        continue;
      }
      matchService.reprocessMatchesForPurpose(referenceId);
      // Recorded after the call so a retry picks up only what is still outstanding
      recomputed.add(referenceId);
    }
  }

  private record Outcome(boolean failed, int matchesRecomputed, boolean retried) {}
}
