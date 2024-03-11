package org.broadinstitute.consent.http.service;

import com.google.cloud.storage.BlobId;
import com.google.inject.Inject;
import java.io.InputStream;
import org.broadinstitute.consent.http.db.DaaDAO;
import org.broadinstitute.consent.http.enumeration.FileCategory;
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

  /**
   * Create a new DataAccessAgreement with all required fields of a FileStorageObject and reqquires
   * that the file content has previously been uploaded to the cloud storage.
   * We don't use the FileStorageObjectService here to prevent circular dependencies.
   * This method will wrap several object creations in a single transaction.
   */
  public DataAccessAgreement createDaaWithFso(Integer userId, Integer dacId, BlobId blobId,
      String fileName, String mediaType, FileCategory category)
      throws Exception {
    FileStorageObject fso = new FileStorageObject();
    fso.setBlobId(blobId);
    fso.setFileName(fileName);
    fso.setCategory(category);
    fso.setMediaType(mediaType);
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
