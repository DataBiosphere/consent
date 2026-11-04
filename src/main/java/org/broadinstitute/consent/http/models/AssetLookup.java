package org.broadinstitute.consent.http.models;

import org.broadinstitute.consent.http.enumeration.AssetType;

/**
 * What a DUOS identifier names. A removed asset carries only its type and status; the other fields
 * are null.
 *
 * @param listed whether the asset's study has a dataset in the search index, without which the
 *     asset is not in the Data Library
 * @param assetId the dataset id, or the model, workspace or biospecimen row id
 */
public record AssetLookup(
    String identifier,
    AssetType type,
    Status status,
    Boolean listed,
    Integer assetId,
    Integer studyId) {

  public enum Status {
    LIVE,
    RETIRED,
    REMOVED
  }

  public static AssetLookup removed(AssetType type) {
    return new AssetLookup(null, type, Status.REMOVED, null, null, null);
  }
}
