package org.broadinstitute.consent.http.service;

import java.util.List;
import java.util.ArrayList;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.models.DarCollection;

public class DarCollectionService {
  private final DarCollectionDAO darCollectionDAO;

  public DarCollectionService(DarCollectionDAO darCollectionDAO) {
    this.darCollectionDAO = darCollectionDAO;
  };

  public List<DarCollection> findDarCollectionsByUserId(Integer userId) {
    // function is stubbed out for resource function
    return new ArrayList<DarCollection>();
  }
}
