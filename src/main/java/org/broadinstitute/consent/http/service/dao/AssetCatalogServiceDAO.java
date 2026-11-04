package org.broadinstitute.consent.http.service.dao;

import com.google.inject.Inject;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.db.AssetCatalogCheckDAO;
import org.broadinstitute.consent.http.db.AssetCatalogDAO;
import org.broadinstitute.consent.http.db.AssetCatalogRepairDAO;
import org.broadinstitute.consent.http.db.AssetCatalogRepairDAO.ChangedEntry;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.models.AssetCatalogCheck;
import org.broadinstitute.consent.http.models.AssetCatalogRepair;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.JdbiException;

public class AssetCatalogServiceDAO implements ConsentLogger {

  static final String REPAIR_LOCK_TIMEOUT = "10s";
  // Lock timeout and deadlock: another write held a table or row the repair needed.
  private static final Set<String> LOCK_FAILURE_STATES = Set.of("55P03", "40P01");

  private final Jdbi jdbi;

  @Inject
  public AssetCatalogServiceDAO(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  /**
   * Fixes what the check reports, in one transaction: relinks entries a restore unlinked, inserts
   * the missing ones, stamps the remaining orphans and advances a lagging alias sequence.
   *
   * @throws ConsentConflictException while another repair runs, or when a write holds a lock the
   *     repair needs
   */
  public AssetCatalogRepair repair() {
    try {
      return jdbi.inTransaction(
          handle -> {
            AssetCatalogRepairDAO repairDAO = handle.attach(AssetCatalogRepairDAO.class);
            AssetCatalogCheckDAO checkDAO = handle.attach(AssetCatalogCheckDAO.class);
            if (!repairDAO.tryRepairLock()) {
              throw new ConsentConflictException("Another asset catalog repair is running.");
            }
            handle.execute("SET LOCAL lock_timeout = '%s'".formatted(REPAIR_LOCK_TIMEOUT));
            // Before any catalog write, in the order the dataset delete paths take them.
            handle.attach(AssetCatalogDAO.class).lockDatasetTable();
            Long sequence =
                checkDAO.findSequenceBehind(1).isEmpty() ? null : repairDAO.advanceAliasSequence();
            List<ChangedEntry> relinked =
                concat(repairDAO.relinkAssetEntries(), repairDAO.relinkDatasetEntries());
            List<ChangedEntry> inserted =
                concat(
                    repairDAO.insertMissingDatasetEntries(), repairDAO.insertMissingAssetEntries());
            List<ChangedEntry> stamped = repairDAO.stampOrphans();
            AssetCatalogRepair repair =
                new AssetCatalogRepair(
                    inserted.size(),
                    relinked.size(),
                    stamped.size(),
                    leftWithoutEntry(checkDAO),
                    sequence);
            handle.afterCommit(() -> logRepair(repair, relinked, inserted, stamped));
            return repair;
          });
    } catch (JdbiException e) {
      if (isLockFailure(e)) {
        throw new ConsentConflictException(
            "A dataset or asset write holds a lock the repair needs; retry the repair.");
      }
      throw e;
    }
  }

  static boolean isLockFailure(Throwable e) {
    for (Throwable cause = e; cause != null; cause = cause.getCause()) {
      // Set.of rejects a null lookup, and a driver may leave the SQLState unset.
      if (cause instanceof SQLException sql
          && sql.getSQLState() != null
          && LOCK_FAILURE_STATES.contains(sql.getSQLState())) {
        return true;
      }
    }
    return false;
  }

  private static long leftWithoutEntry(AssetCatalogCheckDAO checkDAO) {
    return Stream.of(
            checkDAO.findMissing(1), checkDAO.findCollisions(1), checkDAO.findRelinkable(1))
        .mapToLong(rows -> rows.isEmpty() ? 0 : rows.get(0).total())
        .sum();
  }

  private static List<ChangedEntry> concat(List<ChangedEntry> first, List<ChangedEntry> second) {
    return Stream.concat(first.stream(), second.stream()).toList();
  }

  private void logRepair(
      AssetCatalogRepair repair,
      List<ChangedEntry> relinked,
      List<ChangedEntry> inserted,
      List<ChangedEntry> stamped) {
    logInfo("Asset catalog repair: %s".formatted(repair));
    logEntries("relinked", relinked);
    logEntries("inserted", inserted);
    logEntries("stamped", stamped);
  }

  private void logEntries(String action, List<ChangedEntry> entries) {
    entries.stream()
        .limit(AssetCatalogCheck.FINDING_LIMIT)
        .forEach(
            e ->
                logInfo(
                    "Asset catalog repair %s entry %d (%s %d)"
                        .formatted(action, e.catalogId(), e.assetType(), e.assetNumber())));
  }
}
