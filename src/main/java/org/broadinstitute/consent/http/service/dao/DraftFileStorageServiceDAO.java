package org.broadinstitute.consent.http.service.dao;

import com.google.cloud.storage.BlobId;
import com.google.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
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

public class DraftFileStorageServiceDAO implements ConsentLogger {

  private final Jdbi jdbi;
  private final GCSService gcsService;
  private final FileStorageObjectDAO fileStorageObjectDAO;

  @Inject
  public DraftFileStorageServiceDAO(Jdbi jdbi, GCSService gcsService) {
    this.jdbi = jdbi;
    this.gcsService = gcsService;
    this.fileStorageObjectDAO = jdbi.onDemand(FileStorageObjectDAO.class);
  }

  public List<FileStorageObject> storeDraftFiles(
      UUID associatedId, User user, Map<String, FormDataBodyPart> files) {
    List<FileStorageObject> fileStorageObjects = new ArrayList<>();
    jdbi.useTransaction(
        _ -> {
          try {
            files.forEach((key, file) -> fileStorageObjects.add(store(file, user, associatedId)));
          } catch (Exception e) {
            // The transaction drops the file rows; the uploaded documents need deleting here.
            deleteDocuments(fileStorageObjects, associatedId);
            throw e;
          }
        });
    return fileStorageObjects;
  }

  /** Deletes a file's row, then its document, so a failed document delete keeps the row. */
  public void deleteStoredFile(FileStorageObject fileStorageObject, User user)
      throws NotFoundException {
    jdbi.useTransaction(
        _ -> {
          try {
            fileStorageObjectDAO.deleteFileById(
                fileStorageObject.getFileStorageObjectId(), user.getUserId());
            gcsService.deleteDocument(fileStorageObject.getBlobId().getName());
          } catch (Exception e) {
            logWarn(
                String.format(
                    "Error deleting stored file for user: %s, file obj id: %d, error: %s",
                    user.getEmail(), fileStorageObject.getFileStorageObjectId(), e));
            throw new NotFoundException(
                "Error deleting stored file for user: " + user.getEmail(), e);
          }
        });
  }

  /** Deletes the files' rows, in the caller's transaction when there is one. */
  public void deleteFileRows(Collection<FileStorageObject> files, User user) {
    jdbi.useTransaction(
        _ ->
            files.forEach(
                file ->
                    fileStorageObjectDAO.deleteFileById(
                        file.getFileStorageObjectId(), user.getUserId())));
  }

  /**
   * Deletes the files' documents, logging any that fail. Documents cannot be rolled back, so
   * callers delete them only once the rows are gone for good.
   */
  public void deleteDocuments(Collection<FileStorageObject> files, UUID draftId) {
    files.forEach(file -> deleteDocument(file.getBlobId(), draftId));
  }

  private void deleteDocument(BlobId blobId, UUID draftId) {
    try {
      gcsService.deleteDocument(blobId.getName());
    } catch (Exception e) {
      logWarn(
          "Error deleting the document for draft: %s, gcsuri: %s"
              .formatted(draftId, blobId.toGsUtilUri()),
          e);
    }
  }

  private FileStorageObject store(FormDataBodyPart file, User user, UUID draftId) {
    BlobId blobId = null;
    try {
      // upload to GCS
      blobId =
          gcsService.storeDocument(
              file.getValueAs(InputStream.class),
              file.getMediaType().toString(),
              UUID.randomUUID());
      Integer fileStorageObjectId =
          fileStorageObjectDAO.insertNewFile(
              file.getName(),
              FileCategory.DRAFT_UPLOADED_FILE.getValue(),
              blobId.toGsUtilUri(),
              file.getMediaType().toString(),
              draftId.toString(),
              user.getUserId(),
              Instant.now());
      return fileStorageObjectDAO.findFileById(fileStorageObjectId);
    } catch (Exception e) {
      logWarn(
          String.format(
              "Error storing file for user: %s, draft id : %s, error: %s",
              user.getEmail(), draftId.toString(), e));
      if (blobId != null) {
        deleteDocument(blobId, draftId);
      }
      throw new ServerErrorException(
          "Unable to store the file " + file.getName(), Response.Status.INTERNAL_SERVER_ERROR, e);
    }
  }
}
