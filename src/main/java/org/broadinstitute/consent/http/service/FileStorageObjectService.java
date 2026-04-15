package org.broadinstitute.consent.http.service;

import com.google.cloud.storage.BlobId;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.FileStorageObjectDAO;
import org.broadinstitute.consent.http.enumeration.DocumentEntity;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Dataset;
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

  public FileStorageObject updateCategoryByEntityAndEntityIdForWrite(
      User user, String entity, String entityId, Integer fileStorageObjectId, String categoryValue)
      throws NotFoundException {
    if (categoryValue == null || categoryValue.trim().isEmpty()) {
      throw new BadRequestException("Invalid category");
    }

    FileCategory category = FileCategory.findValue(categoryValue.trim());
    if (category == null) {
      throw new BadRequestException("Invalid category");
    }

    validateCategoryAllowedForEntity(entity, category);

    String fsoEntityId = resolveFsoEntityIdForWrite(user, entity, entityId);
    FileStorageObject existingFileStorageObject =
        fileStorageObjectDAO.findFileByIdAndEntityId(fsoEntityId, fileStorageObjectId);
    if (existingFileStorageObject == null
        || Boolean.TRUE.equals(existingFileStorageObject.getDeleted())) {
      throw new NotFoundException("File not found");
    }

    fileStorageObjectDAO.updateCategory(
        fileStorageObjectId, category.getValue(), user.getUserId(), Instant.now());
    return fileStorageObjectDAO.findFileById(fileStorageObjectId);
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

  private String resolveFsoEntityIdForWrite(User user, String entity, String entityId) {
    if ("dac".equalsIgnoreCase(entity) || "dar".equalsIgnoreCase(entity)) {
      if (!user.hasUserRole(UserRoles.ADMIN)) {
        throw new ForbiddenException("User does not have permission");
      }
      Integer numericEntityId = parseNumericEntityId(entityId);
      return numericEntityId.toString();
    }

    DocumentEntity documentEntity =
        DocumentEntity.fromValue(entity)
            .orElseThrow(() -> new NotFoundException("Entity not found"));

    return switch (documentEntity) {
      case DATASET -> resolveDatasetFsoEntityIdForWrite(entityId, user);
      case STUDY -> resolveStudyFsoEntityIdForWrite(entityId, user);
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

  private String resolveDatasetFsoEntityIdForWrite(String entityId, User user) {
    Integer datasetId = parseNumericEntityId(entityId);
    Dataset dataset = datasetService.findDatasetByIdForRead(user, datasetId);
    if (!user.hasUserRole(UserRoles.ADMIN) && !user.getUserId().equals(dataset.getCreateUserId())) {
      throw new ForbiddenException("User does not have permission");
    }
    return datasetId.toString();
  }

  private String resolveStudyFsoEntityIdForWrite(String entityId, User user) {
    Integer studyId = parseNumericEntityId(entityId);
    Study study = datasetService.findStudyByIdForRead(user, studyId);
    if (!datasetService.isCreatorCustodianOrAdmin(user, study)) {
      throw new ForbiddenException("User does not have permission");
    }
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

  private void validateCategoryAllowedForEntity(String entity, FileCategory category) {
    Set<FileCategory> allowedCategories;
    if ("dataset".equalsIgnoreCase(entity) || "study".equalsIgnoreCase(entity)) {
      allowedCategories =
          Set.of(
              FileCategory.IRB_COLLABORATION_LETTER,
              FileCategory.DATA_USE_LETTER,
              FileCategory.ALTERNATIVE_DATA_SHARING_PLAN,
              FileCategory.NIH_INSTITUTIONAL_CERTIFICATION);
    } else if ("dac".equalsIgnoreCase(entity) || "dar".equalsIgnoreCase(entity)) {
      allowedCategories = Set.of(FileCategory.DATA_ACCESS_AGREEMENT);
    } else {
      throw new NotFoundException("Entity not found");
    }

    if (!allowedCategories.contains(category)) {
      throw new BadRequestException(
          String.format(
              "Category '%s' is not allowed for entity '%s'", category.getValue(), entity));
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
