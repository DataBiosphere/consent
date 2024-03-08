package org.broadinstitute.consent.http.service.dao;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.broadinstitute.consent.http.db.DaaDAO;
import org.broadinstitute.consent.http.db.FileStorageObjectDAO;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.jdbi.v3.core.Jdbi;

public class DaaServiceDAO implements ConsentLogger {

  private final Jdbi jdbi;
  private final DaaDAO daaDAO;
  private final FileStorageObjectDAO fsoDAO;

  @Inject
  public DaaServiceDAO(Jdbi jdbi, DaaDAO daaDAO, FileStorageObjectDAO fsoDAO) {
    this.jdbi = jdbi;
    this.daaDAO = daaDAO;
    this.fsoDAO = fsoDAO;
  }

  public Integer createDaaWithFso(Integer userId, Integer dacId, FileStorageObject fso) throws Exception {
    List<Integer> createdDaaIds = new ArrayList<>();
    jdbi.useHandle(handle -> {
      handle.getConnection().setAutoCommit(false);
      Instant now = Instant.now();
      try {
        Integer daaId = daaDAO.createDaa(
            userId,
            now,
            userId,
            now,
            dacId);
        createdDaaIds.add(daaId);
        daaDAO.createDacDaaRelation(dacId, daaId);
        if (fso != null) {
          fsoDAO.insertNewFile(
              fso.getFileName(),
              fso.getCategory().getValue(),
              fso.getBlobId().toGsUtilUri(),
              fso.getMediaType(),
              daaId.toString(),
              userId,
              now);
        }
      } catch (Exception e) {
        handle.rollback();
        logException(e);
        throw e;
      }
      handle.commit();
    });
    return createdDaaIds.isEmpty() ? null : createdDaaIds.get(0);
  }

}
