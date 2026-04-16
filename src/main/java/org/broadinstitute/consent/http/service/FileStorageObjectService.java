package org.broadinstitute.consent.http.service;

import com.google.cloud.storage.BlobId;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.FileStorageObjectDAO;
import org.broadinstitute.consent.http.enumeration.DocumentEntity;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

public class FileStorageObjectService implements ConsentLogger {

  private static final Set<FileCategory> DATASET_AND_STUDY_CATEGORIES =
      EnumSet.of(
          FileCategory.IRB_COLLABORATION_LETTER,
          FileCategory.DATA_USE_LETTER,
          FileCategory.ALTERNATIVE_DATA_SHARING_PLAN,
          FileCategory.NIH_INSTITUTIONAL_CERTIFICATION);
  private static final Set<FileCategory> DAC_AND_DAR_CATEGORIES =
      EnumSet.of(FileCategory.DATA_ACCESS_AGREEMENT);

  GCSService gcsService;
  FileStorageObjectDAO fileStorageObjectDAO;
  DatasetService datasetService;
  DacService dacService;
  DataAccessRequestService dataAccessRequestService;

  public FileStorageObjectService(
      FileStorageObjectDAO fileStorageObjectDAO,
      GCSService gcsService,
      DatasetService datasetService,
      DacService dacService,
      DataAccessRequestService dataAccessRequestService) {
    this.fileStorageObjectDAO = fileStorageObjectDAO;
    this.gcsService = gcsService;
    this.datasetService = datasetService;
    this.dacService = dacService;
    this.dataAccessRequestService = dataAccessRequestService;
  }

  public FileStorageObject uploadDocument(
      User user,
      String entity,
      String entityId,
      InputStream content,
      FormDataContentDisposition fileDetail,
      String categoryStr)
      throws IOException {
    if (content == null || fileDetail == null || fileDetail.getFileName() == null) {
      throw new BadRequestException("File is required");
    }

    FileCategory category = FileCategory.findValue(categoryStr);
    if (category == null) {
      throw new BadRequestException("Invalid category");
    }

    DocumentEntity documentEntity =
        DocumentEntity.fromValue(entity)
            .orElseThrow(() -> new NotFoundException("Entity not found"));

    if (!isCategoryAllowed(documentEntity, category)) {
      throw new BadRequestException("Category is not allowed for entity");
    }

    String resolvedEntityId = resolveFsoEntityIdForWrite(user, documentEntity, entityId);
    return uploadAndStoreFile(
        content,
        fileDetail.getFileName(),
        MediaType.APPLICATION_OCTET_STREAM,
        category,
        resolvedEntityId,
        user.getUserId());
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

    // insert file metadata
    Integer fileStorageObjectId =
        fileStorageObjectDAO.create(
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
      case DAC, DAR -> throw new NotFoundException("Entity not found");
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

  private String resolveFsoEntityIdForWrite(User user, DocumentEntity entity, String entityId) {
    return switch (entity) {
      case DATASET -> resolveDatasetFsoEntityIdForRead(entityId, user);
      case STUDY -> resolveStudyFsoEntityIdForRead(entityId, user);
      case DAC -> resolveDacEntityIdForWrite(user, entityId);
      case DAR -> resolveDarEntityIdForWrite(user, entityId);
    };
  }

  private String resolveDacEntityIdForWrite(User user, String entityId) {
    Integer dacId = parseNumericEntityId(entityId);
    dacService.findById(dacId);
    if (user.hasUserRole(UserRoles.ADMIN)) {
      return dacId.toString();
    }
    boolean hasChairRole =
        user.getRoles().stream()
            .filter(role -> Objects.equals(role.getRoleId(), UserRoles.CHAIRPERSON.getRoleId()))
            .map(UserRole::getDacId)
            .anyMatch(dacId::equals);
    if (!hasChairRole) {
      throw new ForbiddenException("User does not have permission");
    }
    return dacId.toString();
  }

  private String resolveDarEntityIdForWrite(User user, String entityId) {
    DataAccessRequest dar = dataAccessRequestService.findByReferenceId(entityId);
    if (user.hasUserRole(UserRoles.ADMIN)
        || (dar.getUserId() != null && dar.getUserId().equals(user.getUserId()))) {
      return entityId;
    }
    throw new ForbiddenException("User does not have permission");
  }

  private boolean isCategoryAllowed(DocumentEntity entity, FileCategory category) {
    return switch (entity) {
      case DATASET, STUDY -> DATASET_AND_STUDY_CATEGORIES.contains(category);
      case DAC, DAR -> DAC_AND_DAR_CATEGORIES.contains(category);
    };
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
