package org.broadinstitute.consent.http.service.dao;

import com.google.cloud.storage.BlobId;
import com.google.inject.Inject;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
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
  private final ExecutorService executorService;

  @Inject
  public DraftFileStorageServiceDAO(
      Jdbi jdbi, GCSService gcsService, ExecutorService executorService) {
    this.jdbi = jdbi;
    this.gcsService = gcsService;
    this.executorService = executorService;
    this.fileStorageObjectDAO = jdbi.onDemand(FileStorageObjectDAO.class);
  }

  /** A document stored in GCS that has no file row yet. */
  public record UploadedDocument(String fileName, String mediaType, BlobId blobId) {}

  /**
   * Stores the files in GCS in parallel. Call it outside any transaction, so no pooled connection
   * waits on the uploads. A failure deletes the documents that were stored.
   */
  public List<UploadedDocument> uploadDocuments(
      UUID draftId, User user, Map<String, FormDataBodyPart> files) {
    List<CompletableFuture<UploadedDocument>> uploads =
        files.values().stream()
            .map(
                file ->
                    CompletableFuture.supplyAsync(
                        () -> uploadDocument(file, draftId, user), executorService))
            .toList();
    List<UploadedDocument> uploaded = new ArrayList<>();
    RuntimeException failure = null;
    for (CompletableFuture<UploadedDocument> upload : uploads) {
      try {
        uploaded.add(upload.join());
      } catch (CompletionException e) {
        failure = failure == null ? (RuntimeException) e.getCause() : failure;
      }
    }
    if (failure != null) {
      deleteUploads(uploaded, draftId);
      throw failure;
    }
    return uploaded;
  }

  private UploadedDocument uploadDocument(FormDataBodyPart file, UUID draftId, User user) {
    try {
      String mediaType = file.getMediaType().toString();
      BlobId blobId =
          gcsService.storeDocument(
              file.getValueAs(InputStream.class), mediaType, UUID.randomUUID());
      return new UploadedDocument(file.getName(), mediaType, blobId);
    } catch (Exception e) {
      logWarn(
          "Error storing file for user: %d, draft id: %s".formatted(user.getUserId(), draftId), e);
      throw new ServerErrorException(
          "Unable to store the file " + file.getName(), Response.Status.INTERNAL_SERVER_ERROR, e);
    }
  }

  /**
   * Records uploaded documents as the draft's files, in the caller's transaction when there is one.
   */
  public List<FileStorageObject> insertFileRows(
      List<UploadedDocument> documents, UUID draftId, User user) {
    return jdbi.inTransaction(
        _ ->
            fileStorageObjectDAO.insertNewFiles(
                documents.stream().map(UploadedDocument::fileName).toList(),
                FileCategory.DRAFT_UPLOADED_FILE.getValue(),
                documents.stream().map(document -> document.blobId().toGsUtilUri()).toList(),
                documents.stream().map(UploadedDocument::mediaType).toList(),
                draftId.toString(),
                user.getUserId(),
                Instant.now()));
  }

  /** The draft's files that are not deleted, read in the caller's transaction when there is one. */
  public List<FileStorageObject> findActiveFiles(UUID draftId) {
    return fileStorageObjectDAO.findActiveFilesByEntityId(draftId.toString());
  }

  public void deleteUploads(Collection<UploadedDocument> documents, UUID draftId) {
    documents.forEach(document -> deleteDocument(document.blobId(), draftId));
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
}
