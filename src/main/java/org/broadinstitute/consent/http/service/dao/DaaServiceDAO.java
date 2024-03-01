package org.broadinstitute.consent.http.service.dao;

import com.google.inject.Inject;
import java.util.List;
import org.broadinstitute.consent.http.db.DaaDAO;
import org.broadinstitute.consent.http.db.FileStorageObjectDAO;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.jdbi.v3.core.Jdbi;

public class DaaServiceDAO {

  Jdbi jdbi;

  DaaDAO daaDAO;

  FileStorageObjectDAO fileStorageObjectDAO;

  @Inject
  public DaaServiceDAO(Jdbi jdbi, DaaDAO daaDAO) {
    this.jdbi = jdbi;
    this.daaDAO = daaDAO;
  }

  public void insertDAAWithAssociations(DataAccessAgreement daa, List<Dac> dacList) {
    jdbi.useTransaction(transactionHandle -> {
      DaaDAO daaDAOT = transactionHandle.attach(DaaDAO.class);
//      FileStorageObjectDAO fsoDAOT = transactionHandle.attach(FileStorageObjectDAO.class);
      daaDAOT.createDaa(daa.getCreateUserId(), daa.getCreateDate(), daa.getUpdateUserId(), daa.getUpdateDate(), daa.getInitialDacId());
      for (Dac dac : dacList) {
        daaDAOT.createDaaDacRelation(daa.getId(), dac.getDacId());
      }
//      fsoDAOT.insertNewFile(... stuff ...)
    });
  }

}