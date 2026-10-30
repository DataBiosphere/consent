package org.broadinstitute.consent.http.models;

import java.sql.Timestamp;
import org.broadinstitute.consent.http.enumeration.AssetType;

/** A row of asset_catalog. At most one of the four references is set, and only for its type. */
public record AssetCatalogEntry(
    Integer catalogId,
    AssetType assetType,
    Integer assetNumber,
    Timestamp createDate,
    Timestamp assetRemovedDate,
    Integer sourceDatasetId,
    String backfillRun,
    Integer datasetId,
    Integer modelId,
    Integer workspaceId,
    Integer biospecimenId) {}
