package org.broadinstitute.consent.http.service;

import com.google.cloud.storage.BlobId;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.FileStorageObjectDAO;
import org.broadinstitute.consent.http.enumeration.DocumentEntity;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.util.ConsentLogger;

public class FileStorageObjectService implements ConsentLogger {

  GCSService gcsService;
  FileStorageObjectDAO fileStorageObjectDAO;
  DatasetService datasetService;

  public FileStorageObjectService(
      FileStorageObjectDAO fileStorageObjectDAO,
      GCSService gcsService,
      DatasetService datasetService) {
    this.fileStorageObjectDAO = fileStorageObjectDAO;
    this.gcsService = gcsService;
    this.datasetService = datasetService;
  }

  FileStorageObject uploadAndStoreFile(
      InputStream content,
      String fileName,
      String mediaType,
      FileCategory category,
      String entityId,
      Integer createUserId)
      throws IOException {

    BlobId blobId;
    try {
      // upload to GCS
      blobId = gcsService.storeDocument(content, mediaType, UUID.randomUUID());
    } catch (Exception e) {
      logWarn("Failed to upload file for user id " + createUserId + ": " + e.getMessage());
      throw e;
    }

    // insert file
    Integer fileStorageObjectId =
        fileStorageObjectDAO.insertNewFile(
            fileName,
            category.getValue(),
            blobId.toGsUtilUri(),
            mediaType,
            entityId,
            createUserId,
            Instant.now());

    return fileStorageObjectDAO.findFileById(fileStorageObjectId);
  }

  // fetches file from GCS and adds it to the fileStorageObject
  private void fetchAndPopulateUploadedFile(FileStorageObject fileStorageObject)
      throws NotFoundException {
    try {
      InputStream document = gcsService.getDocument(fileStorageObject.getBlobId());
      fileStorageObject.setUploadedFile(document);
    } catch (NotFoundException e) {
      throw e; // pass along
    } catch (Exception e) {
      // all other exceptions
      logWarn("Failed to get document from GCS: " + e.getMessage());
      throw e;
    }
  }

  private void fetchAndPopulateMultipleUploadedFiles(List<FileStorageObject> fileStorageObjects)
      throws NotFoundException {
    try {
      Map<BlobId, InputStream> documentMap =
          gcsService.getDocuments(
              fileStorageObjects.stream().map(FileStorageObject::getBlobId).toList());

      fileStorageObjects.forEach(fso -> fso.setUploadedFile(documentMap.get(fso.getBlobId())));
    } catch (NotFoundException e) {
      throw e; // pass along
    } catch (Exception e) {
      // all other exceptions
      logWarn("Failed to get document from GCS: " + e.getMessage());
      throw e;
    }
  }

  public FileStorageObject fetchById(Integer fileStorageObjectId) throws NotFoundException {
    FileStorageObject fileStorageObject = fileStorageObjectDAO.findFileById(fileStorageObjectId);
    // download file from GCS
    fetchAndPopulateUploadedFile(fileStorageObject);
    return fileStorageObject;
  }

  public FileStorageObject fetchMetadataByEntityIdAndId(
      String entityId, Integer fileStorageObjectId) throws NotFoundException {
    FileStorageObject fileStorageObject =
        fileStorageObjectDAO.findActiveFileByIdAndEntityId(entityId, fileStorageObjectId);
    if (fileStorageObject == null) {
      throw new NotFoundException("File not found");
    }
    return fileStorageObject;
  }

  public List<FileStorageObject> fetchAllMetadataByEntityAndEntityIdForRead(
      User user, String entity, String entityId) throws NotFoundException {
    String fsoEntityId = resolveFsoEntityIdForRead(user, entity, entityId);
    return fetchAllMetadataByEntityId(fsoEntityId);
  }

  public FileStorageObject fetchMetadataByEntityAndEntityIdForRead(
      User user, String entity, String entityId, Integer fileStorageObjectId)
      throws NotFoundException {
    String fsoEntityId = resolveFsoEntityIdForRead(user, entity, entityId);
    return fetchMetadataByEntityIdAndId(fsoEntityId, fileStorageObjectId);
  }

  private String resolveFsoEntityIdForRead(User user, String entity, String entityId) {
    DocumentEntity documentEntity =
        DocumentEntity.fromValue(entity)
            .orElseThrow(() -> new NotFoundException("Entity not found"));

    return switch (documentEntity) {
      case DATASET -> resolveDatasetFsoEntityIdForRead(entityId, user);
      case STUDY -> resolveStudyFsoEntityIdForRead(entityId, user);
    };
  }

  private String resolveDatasetFsoEntityIdForRead(String entityId, User user) {
    Integer datasetId = parseNumericEntityId(entityId);
    datasetService.findDatasetByIdForRead(user, datasetId);
    return datasetId.toString();
  }

  private String resolveStudyFsoEntityIdForRead(String entityId, User user) {
    Integer studyId = parseNumericEntityId(entityId);
    Study study = datasetService.findStudyByIdForRead(user, studyId);
    if (study.getUuid() == null) {
      throw new NotFoundException("Entity not found");
    }
    return study.getUuid().toString();
  }

  private Integer parseNumericEntityId(String entityId) {
    try {
      return Integer.valueOf(entityId);
    } catch (NumberFormatException _) {
      throw new NotFoundException("Entity not found");
    }
  }

  public List<FileStorageObject> fetchAllByEntityId(String entityId) throws NotFoundException {
    List<FileStorageObject> fileStorageObjects = fileStorageObjectDAO.findFilesByEntityId(entityId);
    // download all files from GCS
    fetchAndPopulateMultipleUploadedFiles(fileStorageObjects);
    return fileStorageObjects;
  }

  public List<FileStorageObject> fetchAllByEntityIdAndCategory(
      String entityId, FileCategory category) throws NotFoundException {
    List<FileStorageObject> fileStorageObjects =
        fileStorageObjectDAO.findFilesByEntityIdAndCategory(entityId, category.getValue());
    // download all files from GCS
    fetchAndPopulateMultipleUploadedFiles(fileStorageObjects);
    return fileStorageObjects;
  }

  public List<FileStorageObject> fetchAllMetadataByEntityId(String entityId) {
    return fileStorageObjectDAO.findFileMetadataByEntityId(entityId);
  }
}
