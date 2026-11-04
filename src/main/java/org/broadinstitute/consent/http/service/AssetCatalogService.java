package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.AssetCatalogCheckDAO;
import org.broadinstitute.consent.http.models.AssetCatalogCheck;
import org.broadinstitute.consent.http.models.AssetCatalogRepair;
import org.broadinstitute.consent.http.service.dao.AssetCatalogServiceDAO;
import org.jdbi.v3.core.Jdbi;

/** On-demand integrity check and repair of asset_catalog; nothing runs them on a schedule. */
public class AssetCatalogService {

  private final AssetCatalogCheckDAO checkDAO;
  private final AssetCatalogServiceDAO assetCatalogServiceDAO;

  @Inject
  public AssetCatalogService(Jdbi jdbi, AssetCatalogServiceDAO assetCatalogServiceDAO) {
    this.checkDAO = jdbi.onDemand(AssetCatalogCheckDAO.class);
    this.assetCatalogServiceDAO = assetCatalogServiceDAO;
  }

  public AssetCatalogCheck check() {
    return checkDAO.check();
  }

  public AssetCatalogRepair repair() {
    return assetCatalogServiceDAO.repair();
  }
}
