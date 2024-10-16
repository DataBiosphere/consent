package org.broadinstitute.consent.http.service;

import com.google.cloud.storage.BlobId;
import com.google.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.FileStorageObjectDAO;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.jdbi.v3.core.Jdbi;

public class DraftFileStorageService implements ConsentLogger {

  Jdbi jdbi;
  GCSService gcsService;
  FileStorageObjectDAO fileStorageObjectDAO;

  @Inject
  public DraftFileStorageService(Jdbi jdbi, GCSService gcsService,
      FileStorageObjectDAO fileStorageObjectDAO) {
    this.jdbi = jdbi;
    this.gcsService = gcsService;
    this.fileStorageObjectDAO = fileStorageObjectDAO;
  }

  public List<FileStorageObject> storeDraftFiles(UUID associatedId, User user,
      Map<String, FormDataBodyPart> files)
      throws SQLException {
    List<FileStorageObject> fileStorageObjects = new ArrayList<>();
    jdbi.useHandle(handle -> {
      handle.getConnection().setAutoCommit(false);
      try {
        files.forEach((String key, FormDataBodyPart file) -> {
          try {
            fileStorageObjects.add(store(file, user, associatedId));
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
      } catch (Exception e) {
        fileStorageObjects.forEach(file -> {
          try {
            deleteStoredFile(file, user);
          } catch (SQLException ex) {
            logWarn(String.format("Error rolling back files in GCS for draft: %s, gcsuri: %s",
                associatedId.toString(), file.getBlobId().toGsUtilUri()));
            throw new RuntimeException(ex);
          }
        });
        handle.getConnection().rollback();
      }
      handle.commit();
    });
    return fileStorageObjects;
  }

  public void deleteStoredFile(FileStorageObject fileStorageObject, User user) throws SQLException {
    jdbi.useHandle(handle -> {
      handle.getConnection().setAutoCommit(false);
      try {
        gcsService.deleteDocument(fileStorageObject.getBlobId().getName());
        fileStorageObjectDAO.deleteFileById(fileStorageObject.getFileStorageObjectId(),
            user.getUserId(), new Date().toInstant());
      } catch (Exception e) {
        logWarn(String.format("Error deleting stored file for user: %s, file obj id: %d, error: %s",
            user.getEmail(), fileStorageObject.getFileStorageObjectId(), e));
        handle.rollback();
        throw new NotFoundException("Error deleting stored file for user: " + user.getEmail(), e);
      }
      handle.commit();
    });
  }

  private FileStorageObject store(FormDataBodyPart file, User user, UUID draftId)
      throws RuntimeException {
    BlobId blobId;
    try {
      // upload to GCS
      blobId = gcsService.storeDocument(
          file.getValueAs(InputStream.class),
          file.getMediaType().toString(),
          UUID.randomUUID());
      Integer fileStorageObjectId = fileStorageObjectDAO.insertNewFile(
          file.getName(),
          FileCategory.DRAFT_UPLOADED_FILE.getValue(),
          blobId.toGsUtilUri(),
          file.getMediaType().toString(),
          draftId.toString(),
          user.getUserId(),
          Instant.now()
      );
      return fileStorageObjectDAO.findFileById(fileStorageObjectId);
    } catch (Exception e) {
      logWarn(String.format("Error storing file for user: %s, draft id : %s, error: %s",
          user.getEmail(), draftId.toString(), e));
      throw new RuntimeException(e);
    }
  }

  public InputStream get(FileStorageObject fileStorageObject) {
    return gcsService.getDocument(fileStorageObject.getBlobId());
  }
}
