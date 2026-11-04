package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.broadinstitute.consent.http.db.AssetCatalogDAO;
import org.broadinstitute.consent.http.db.AssetLookupDAO;
import org.broadinstitute.consent.http.db.AssetLookupDAO.AssetRow;
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
      throw unknown();
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

  /** A denial answers exactly as an unknown identifier does, so the lookup cannot probe. */
  private static void readable(Runnable gate) {
    try {
      gate.run();
    } catch (NotFoundException e) {
      throw unknown();
    }
  }

  private AssetLookup removedOrNotFound(AssetIdentifier identifier) {
    AssetCatalogEntry entry =
        catalogDAO.findByTypeAndNumber(identifier.type(), identifier.number());
    // A linked entry here was registered after the row read, so it must not skip the study gate.
    if (entry == null || !entry.isRemoved()) {
      throw unknown();
    }
    return AssetLookup.removed(identifier.type());
  }

  private static NotFoundException unknown() {
    return new NotFoundException(UNKNOWN_IDENTIFIER);
  }
}
