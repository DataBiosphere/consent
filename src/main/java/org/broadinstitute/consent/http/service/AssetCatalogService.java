package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.AssetCatalogCheckDAO;
import org.broadinstitute.consent.http.models.AssetCatalogCheck;
import org.jdbi.v3.core.Jdbi;

/** On-demand integrity check of asset_catalog; nothing runs it on a schedule. */
public class AssetCatalogService {

  private final AssetCatalogCheckDAO checkDAO;

  @Inject
  public AssetCatalogService(Jdbi jdbi) {
    this.checkDAO = jdbi.onDemand(AssetCatalogCheckDAO.class);
  }

  public AssetCatalogCheck check() {
    return checkDAO.check();
  }
}
