package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import java.util.Optional;
import org.broadinstitute.consent.http.db.AssetCatalogDAO;
import org.broadinstitute.consent.http.db.AssetLookupDAO;
import org.broadinstitute.consent.http.db.AssetLookupDAO.AssetRow;
import org.broadinstitute.consent.http.db.AssetLookupDAO.DatasetRow;
import org.broadinstitute.consent.http.enumeration.AssetType;
import org.broadinstitute.consent.http.models.AssetCatalogEntry;
import org.broadinstitute.consent.http.models.AssetIdentifier;
import org.broadinstitute.consent.http.models.AssetLookup;
import org.broadinstitute.consent.http.models.AssetLookup.Status;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.Jdbi;

/**
 * Resolves a DUOS identifier. A row holding the number wins over the catalog, since that row is
 * what shows the identifier; an unreadable live or retired asset is reported as not found, like an
 * unknown identifier.
 */
public class AssetLookupService {

  static final String UNKNOWN_IDENTIFIER = "Unknown DUOS identifier.";

  private final AssetLookupDAO lookupDAO;
  private final AssetCatalogDAO catalogDAO;
  private final DatasetService datasetService;

  @Inject
  public AssetLookupService(Jdbi jdbi, DatasetService datasetService) {
    this.lookupDAO = jdbi.onDemand(AssetLookupDAO.class);
    this.catalogDAO = jdbi.onDemand(AssetCatalogDAO.class);
    this.datasetService = datasetService;
  }

  /**
   * @throws NotFoundException for an unknown or malformed identifier, or one naming a live or
   *     retired asset the user cannot read
   */
  public AssetLookup lookup(String text, User user) {
    AssetIdentifier identifier = parse(text);
    if (identifier.type() == AssetType.DATASET) {
      return dataset(identifier.number())
          .map(row -> readableDataset(row, user))
          .orElseGet(() -> removedOrNotFound(identifier));
    }
    AssetRow row = lookupDAO.findAssetRow(identifier.type(), identifier.number());
    if (row == null) {
      return removedOrNotFound(identifier);
    }
    readable(() -> datasetService.requireReadableStudy(row.studyId(), user));
    return new AssetLookup(
        identifier.format(),
        identifier.type(),
        row.retiredDate() == null ? Status.LIVE : Status.RETIRED,
        lookupDAO.hasIndexedDataset(row.studyId()),
        row.rowId(),
        row.studyId());
  }

  private static AssetIdentifier parse(String text) {
    try {
      return AssetIdentifier.parse(text);
    } catch (IllegalArgumentException e) {
      throw unknown();
    }
  }

  /**
   * The dataset holding the alias, or else the one an entry still links, under its current alias.
   */
  private Optional<DatasetRow> dataset(int alias) {
    DatasetRow byAlias = lookupDAO.findDatasetByAlias(alias);
    if (byAlias != null) {
      return Optional.of(byAlias);
    }
    AssetCatalogEntry entry = catalogDAO.findByTypeAndNumber(AssetType.DATASET, alias);
    return entry == null || entry.datasetId() == null
        ? Optional.empty()
        : Optional.ofNullable(lookupDAO.findDatasetById(entry.datasetId()));
  }

  private AssetLookup readableDataset(DatasetRow row, User user) {
    readable(() -> datasetService.findDatasetByIdForRead(user, row.datasetId()));
    return new AssetLookup(
        AssetIdentifier.format(AssetType.DATASET, row.alias()),
        AssetType.DATASET,
        Status.LIVE,
        row.studyId() == null
            ? row.indexedDate() != null
            : lookupDAO.hasIndexedDataset(row.studyId()),
        row.datasetId(),
        row.studyId());
  }

  /** A denial answers exactly as an unknown identifier does, so the lookup cannot probe. */
  private static void readable(Runnable gate) {
    try {
      gate.run();
    } catch (NotFoundException | ForbiddenException e) {
      throw unknown();
    }
  }

  private AssetLookup removedOrNotFound(AssetIdentifier identifier) {
    if (catalogDAO.findByTypeAndNumber(identifier.type(), identifier.number()) == null) {
      throw unknown();
    }
    return AssetLookup.removed(identifier.type());
  }

  private static NotFoundException unknown() {
    return new NotFoundException(UNKNOWN_IDENTIFIER);
  }
}
