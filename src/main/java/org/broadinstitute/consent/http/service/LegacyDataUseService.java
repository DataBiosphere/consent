package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.broadinstitute.consent.http.db.PersistedDataUseDAO;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.datause.LegacyDataUseDisposition;
import org.broadinstitute.consent.http.models.datause.LegacyDataUseRunReport;
import org.broadinstitute.consent.http.models.datause.PersistedDataUseClassifier;
import org.broadinstitute.consent.http.models.datause.PersistedDataUseReport;
import org.broadinstitute.consent.http.models.datause.PersistedDataUseRow;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.jdbi.v3.core.Jdbi;

/**
 * Applies approved dispositions to legacy Data Use records and recomputes the matches they affect.
 *
 * <p>Restartable by construction, not by checkpoint: a record already holding its approved value is
 * skipped, so a rerun only touches what is outstanding. Writes reuse {@link
 * DatasetService#updateDatasetDataUse} for validation, translation, audit, and search sync; matches
 * are recomputed per DAR, which never touches elections or votes.
 */
public class LegacyDataUseService implements ConsentLogger {

  private static final String REASON_VALIDATION = "validation";
  private static final String REASON_NOT_FOUND = "not-found";
  private static final String REASON_UNEXPECTED = "unexpected";

  private final PersistedDataUseDAO persistedDataUseDAO;
  private final DatasetService datasetService;
  private final MatchService matchService;

  @Inject
  public LegacyDataUseService(Jdbi jdbi, DatasetService datasetService, MatchService matchService) {
    this(jdbi.onDemand(PersistedDataUseDAO.class), datasetService, matchService);
  }

  LegacyDataUseService(
      PersistedDataUseDAO persistedDataUseDAO,
      DatasetService datasetService,
      MatchService matchService) {
    this.persistedDataUseDAO = persistedDataUseDAO;
    this.datasetService = datasetService;
    this.matchService = matchService;
  }

  /** Redacted classification counts over every persisted Data Use, for pre/post reconciliation. */
  public PersistedDataUseReport report() {
    return PersistedDataUseReport.from(persistedDataUseDAO.findAllPersistedDataUse());
  }

  /** Datasets whose shape the canonical validator would now reject. */
  public List<PersistedDataUseRow> findNoncanonicalRows() {
    return persistedDataUseDAO.findAllPersistedDataUse().stream()
        .filter(row -> !row.isCanonical())
        .toList();
  }

  /** Abstaining datasets reachable through a DAR, which is the unit recompute works by. */
  public List<PersistedDataUseRow> findRowsNeedingMatchRecompute() {
    return persistedDataUseDAO.findAllPersistedDataUse().stream()
        .filter(PersistedDataUseRow::needsMatchRecompute)
        .toList();
  }

  /** Changes no stored Data Use, so it needs no approved disposition. */
  public LegacyDataUseRunReport recomputeAbstainingMatches(User admin) {
    List<PersistedDataUseRow> abstaining =
        persistedDataUseDAO.findAllPersistedDataUse().stream()
            .filter(row -> row.classification().abstainsWhenMatched())
            .toList();
    List<PersistedDataUseRow> candidates =
        abstaining.stream().filter(PersistedDataUseRow::needsMatchRecompute).toList();
    // Unreachable rows are named rather than folded into "skipped", which would read as done
    logInfo(
        "Recomputing matches for %d abstaining datasets; %d have no DAR relation and are left alone"
            .formatted(candidates.size(), abstaining.size() - candidates.size()));
    return run(admin, candidates, _ -> new LegacyDataUseDisposition.RecomputeMatchesOnly());
  }

  /**
   * @param dispositions the approved decision per record; nothing is inferred, so a record without
   *     an approved disposition must be given {@link LegacyDataUseDisposition.Defer}
   */
  public LegacyDataUseRunReport run(
      User admin,
      List<PersistedDataUseRow> candidates,
      Function<PersistedDataUseRow, LegacyDataUseDisposition> dispositions) {
    int processed = 0;
    int skipped = 0;
    int retried = 0;
    int matchesRecomputed = 0;
    Map<String, Integer> failuresByReason = new HashMap<>();
    List<Integer> failedDatasetIds = new ArrayList<>();

    for (PersistedDataUseRow candidate : candidates) {
      LegacyDataUseDisposition disposition = dispositions.apply(candidate);
      if (disposition == null || disposition instanceof LegacyDataUseDisposition.Defer) {
        skipped++;
        continue;
      }
      if (isAlreadyApplied(candidate, disposition)) {
        skipped++;
        continue;
      }

      Outcome outcome = applyWithOneRetry(admin, candidate, disposition);
      if (outcome.retried()) {
        retried++;
      }
      if (outcome.reason() == null) {
        processed++;
        matchesRecomputed += outcome.matchesRecomputed();
      } else {
        failuresByReason.merge(outcome.reason(), 1, Integer::sum);
        failedDatasetIds.add(candidate.datasetId());
      }
    }

    return new LegacyDataUseRunReport(
        processed,
        skipped,
        failedDatasetIds.size(),
        retried,
        matchesRecomputed,
        failuresByReason,
        failedDatasetIds);
  }

  /**
   * A normalization whose value is already stored has nothing to do; a recompute always runs.
   * Compares the values, not their classifications, which two different disease lists would share.
   */
  private boolean isAlreadyApplied(
      PersistedDataUseRow candidate, LegacyDataUseDisposition disposition) {
    if (!(disposition instanceof LegacyDataUseDisposition.Normalize normalize)) {
      return false;
    }
    return PersistedDataUseClassifier.parse(candidate.dataUse())
        .filter(stored -> Objects.equals(stored, normalize.approvedDataUse()))
        .isPresent();
  }

  private Outcome applyWithOneRetry(
      User admin, PersistedDataUseRow candidate, LegacyDataUseDisposition disposition) {
    try {
      return new Outcome(null, apply(admin, candidate, disposition), false);
    } catch (BadRequestException _) {
      // A rejected approved value is a decision to correct, not a transient fault
      return new Outcome(REASON_VALIDATION, 0, false);
    } catch (NotFoundException _) {
      return new Outcome(REASON_NOT_FOUND, 0, false);
    } catch (Exception _) {
      logWarn(
          "Legacy Data Use action failed for dataset %d, retrying once"
              .formatted(candidate.datasetId()));
      try {
        return new Outcome(null, apply(admin, candidate, disposition), true);
      } catch (Exception _) {
        // No message or cause: a failure raised while writing can quote the Other free text
        logWarn(
            "Legacy Data Use action failed again for dataset %d".formatted(candidate.datasetId()));
        return new Outcome(REASON_UNEXPECTED, 0, true);
      }
    }
  }

  private int apply(
      User admin, PersistedDataUseRow candidate, LegacyDataUseDisposition disposition) {
    if (disposition instanceof LegacyDataUseDisposition.Normalize normalize) {
      datasetService.updateDatasetDataUse(
          admin, candidate.datasetId(), normalize.approvedDataUse());
    }
    return recomputeMatches(candidate.datasetId());
  }

  private int recomputeMatches(Integer datasetId) {
    List<String> referenceIds = persistedDataUseDAO.findDarReferenceIdsByDatasetId(datasetId);
    referenceIds.forEach(matchService::reprocessMatchesForPurpose);
    return referenceIds.size();
  }

  private record Outcome(String reason, int matchesRecomputed, boolean retried) {}
}
