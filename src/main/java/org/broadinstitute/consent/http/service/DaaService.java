package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.DaaDAO;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.service.dao.DaaServiceDAO;

public class DaaService {

  private final DaaServiceDAO daaServiceDAO;
  private final DaaDAO daaDAO;

  @Inject
  public DaaService(DaaServiceDAO daaServiceDAO, DaaDAO daaDAO) {
    this.daaServiceDAO = daaServiceDAO;
    this.daaDAO = daaDAO;
  }

  public DataAccessAgreement createDaaWithFso(Integer userId, Integer dacId, FileStorageObject fso)
      throws Exception {
    Integer daaId = daaServiceDAO.createDaaWithFso(userId, dacId, fso);
    return daaDAO.findById(daaId);
  }

  public void addDacToDaa(Integer dacId, Integer daaId) {
    daaDAO.createDacDaaRelation(dacId, daaId);
  }

  public void removeDacFromDaa(Integer dacId, Integer daaId) {
    daaDAO.deleteDacDaaRelation(dacId, daaId);
  }

}
