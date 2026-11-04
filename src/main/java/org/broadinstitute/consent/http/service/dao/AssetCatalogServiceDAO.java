package org.broadinstitute.consent.http.service.dao;

import com.google.inject.Inject;
import java.util.List;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.db.AssetCatalogCheckDAO;
import org.broadinstitute.consent.http.db.AssetCatalogDAO;
import org.broadinstitute.consent.http.db.AssetCatalogRepairDAO;
import org.broadinstitute.consent.http.db.AssetCatalogRepairDAO.ChangedEntry;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.models.AssetCatalogRepair;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.jdbi.v3.core.Jdbi;

public class AssetCatalogServiceDAO implements ConsentLogger {

  private final Jdbi jdbi;

  @Inject
  public AssetCatalogServiceDAO(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  /**
   * Fixes what the check reports, in one transaction: relinks entries a restore unlinked, inserts
   * the missing ones, stamps the remaining orphans and advances a lagging alias sequence.
   *
   * @throws ConsentConflictException while another repair runs
   */
  public AssetCatalogRepair repair() {
    return jdbi.inTransaction(
        handle -> {
          AssetCatalogRepairDAO repairDAO = handle.attach(AssetCatalogRepairDAO.class);
          AssetCatalogCheckDAO checkDAO = handle.attach(AssetCatalogCheckDAO.class);
          if (!repairDAO.tryRepairLock()) {
            throw new ConsentConflictException("Another asset catalog repair is running.");
          }
          List<ChangedEntry> relinked =
              concat(repairDAO.relinkAssetEntries(), repairDAO.relinkDatasetEntries());
          List<ChangedEntry> inserted =
              concat(
                  repairDAO.insertMissingDatasetEntries(), repairDAO.insertMissingAssetEntries());
          List<ChangedEntry> stamped = repairDAO.stampOrphans();
          Long sequence = null;
          if (!checkDAO.findSequenceBehind(1).isEmpty()) {
            handle.attach(AssetCatalogDAO.class).lockDatasetTable();
            sequence = repairDAO.advanceAliasSequence();
          }
          relinked.forEach(e -> logRepair("relinked", e));
          inserted.forEach(e -> logRepair("inserted", e));
          stamped.forEach(e -> logRepair("stamped", e));
          if (sequence != null) {
            logInfo("Asset catalog repair advanced dataset_alias_seq to %d".formatted(sequence));
          }
          return new AssetCatalogRepair(
              inserted.size(),
              relinked.size(),
              stamped.size(),
              leftWithoutEntry(checkDAO),
              sequence);
        });
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

  private void logRepair(String action, ChangedEntry entry) {
    logInfo(
        "Asset catalog repair %s entry %d (%s %d)"
            .formatted(action, entry.catalogId(), entry.assetType(), entry.assetNumber()));
  }
}
