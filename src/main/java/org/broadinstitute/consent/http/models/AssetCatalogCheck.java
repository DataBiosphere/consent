package org.broadinstitute.consent.http.models;

import java.util.List;
import org.broadinstitute.consent.http.enumeration.AssetType;

/**
 * Drift between asset_catalog and the dataset and asset tables, one count and the first findings of
 * each kind. A finding names the entry ({@code catalogId}), the row ({@code rowId}: a dataset id or
 * an asset row id), or both.
 */
public record AssetCatalogCheck(
    Findings orphans,
    Findings missing,
    Findings collisions,
    Findings mismatches,
    Findings relinkable,
    Findings sequenceBehind) {

  public static final int FINDING_LIMIT = 500;

  public record Findings(long count, List<Finding> items) {}

  public record Finding(
      AssetType assetType, Integer assetNumber, Integer catalogId, Integer rowId) {}
}
