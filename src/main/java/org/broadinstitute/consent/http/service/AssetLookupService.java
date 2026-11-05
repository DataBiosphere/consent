package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import java.util.function.Supplier;
import org.broadinstitute.consent.http.db.AssetCatalogDAO;
import org.broadinstitute.consent.http.db.AssetLookupDAO;
import org.broadinstitute.consent.http.db.AssetLookupDAO.AssetRow;
import org.broadinstitute.consent.http.enumeration.AssetType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AssetCatalogEntry;
import org.broadinstitute.consent.http.models.AssetIdentifier;
import org.broadinstitute.consent.http.models.AssetLookup;
import org.broadinstitute.consent.http.models.AssetLookup.Status;
import org.broadinstitute.consent.http.models.Dataset;
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
   *     retired asset the user cannot read, or a removed asset for a non-admin
   */
  public AssetLookup lookup(String text, User user) {
    AssetIdentifier identifier = parse(text);
    if (identifier.type() == AssetType.DATASET) {
      Integer datasetId = datasetId(identifier.number());
      return datasetId == null
          ? removedOrNotFound(identifier, user)
          : readableDataset(identifier, datasetId, user);
    }
    AssetRow row = lookupDAO.findAssetRow(identifier.type(), identifier.number());
    if (row == null) {
      return removedOrNotFound(identifier, user);
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

  /** The dataset holding the alias, or else the one an entry still links. */
  private Integer datasetId(int alias) {
    Integer byAlias = lookupDAO.findDatasetIdByAlias(alias);
    if (byAlias != null) {
      return byAlias;
    }
    AssetCatalogEntry entry = catalogDAO.findByTypeAndNumber(AssetType.DATASET, alias);
    return entry == null ? null : entry.datasetId();
  }

  /** Answers from the dataset the read rule checked, under its current alias. */
  private AssetLookup readableDataset(AssetIdentifier identifier, int datasetId, User user) {
    Dataset dataset;
    try {
      dataset = datasetService.findDatasetWithoutFilesForRead(user, datasetId);
    } catch (NotFoundException e) {
      // Deleted after its id was read.
      return removedOrNotFound(identifier, user);
    } catch (ForbiddenException e) {
      throw unknown();
    }
    return new AssetLookup(
        AssetIdentifier.format(AssetType.DATASET, dataset.getAlias()),
        AssetType.DATASET,
        Status.LIVE,
        dataset.getIndexedDate() != null,
        dataset.getDatasetId(),
        dataset.getStudyId());
  }

  /** A denial answers exactly as an unknown identifier does, so the lookup cannot probe. */
  private static <T> T readable(Supplier<T> gate) {
    try {
      return gate.get();
    } catch (NotFoundException | ForbiddenException e) {
      throw unknown();
    }
  }

  /** Removed identifiers are admin-only until product confirms showing them to every user. */
  private AssetLookup removedOrNotFound(AssetIdentifier identifier, User user) {
    if (!user.hasUserRole(UserRoles.ADMIN)) {
      throw unknown();
    }
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
