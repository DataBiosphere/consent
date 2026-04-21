package org.broadinstitute.consent.http.service;

import com.google.cloud.storage.BlobId;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.FileStorageObjectDAO;
import org.broadinstitute.consent.http.enumeration.DocumentEntity;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.enumeration.OperationType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

/**
 * Service layer for FileStorageObject operations.
 *
 * <p>Centralizes ALL business logic and authorization. The resource layer delegates completely to
 * this service.
 *
 * <p>Authorization entry point: {@link #checkAccess(User, String, String, FileCategory,
 * OperationType)}
 */
public class FileStorageObjectService implements ConsentLogger {

  private static final String ENTITY_NOT_FOUND = "Entity not found";
  private static final String PERMISSION_DENIED = "User does not have permission";

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

  // ---------------------------------------------------------------------------
  // Public API
  // ---------------------------------------------------------------------------

  /**
   * Uploads a new document for the given entity.
   *
   * <ol>
   *   <li>Validates file content.
   *   <li>Parses and validates the entity + category.
   *   <li>Calls {@link #checkAccess} (WRITE).
   *   <li>Resolves entity ID and stores the file.
   * </ol>
   */
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

    DocumentEntity documentEntity = requireDocumentEntity(entity);
    FileCategory category = requireValidCategory(categoryStr);
    validateCategoryForEntity(documentEntity, category);
    checkAccess(user, entity, entityId, category, OperationType.WRITE);

    String resolvedEntityId = resolveEntityId(user, documentEntity, entityId);
    return uploadAndStoreFile(
        content,
        fileDetail.getFileName(),
        MediaType.APPLICATION_OCTET_STREAM,
        category,
        resolvedEntityId,
        user.getUserId());
  }

  /**
   * Returns metadata for a single document.
   *
   * <ol>
   *   <li>Calls {@link #checkAccess} with a null category (entity-level READ).
   *   <li>Resolves entity ID.
   *   <li>Fetches FSO metadata.
   * </ol>
   */
  public FileStorageObject getDocument(
      User user, String entity, String entityId, Integer fileStorageObjectId) {
    DocumentEntity documentEntity = requireDocumentEntity(entity);
    checkAccess(user, entity, entityId, null, OperationType.READ);

    String resolvedEntityId = resolveEntityId(user, documentEntity, entityId);
    return fetchMetadataByEntityIdAndId(resolvedEntityId, fileStorageObjectId);
  }

  /**
   * Downloads the binary content of a document.
   *
   * <ol>
   *   <li>Resolves entity ID.
   *   <li>Fetches FSO metadata (including category).
   *   <li>Calls {@link #checkAccess} with the file's actual category (READ).
   *   <li>Streams file from GCS.
   * </ol>
   */
  public FileStorageObject getDocumentFile(
      User user, String entity, String entityId, Integer fileStorageObjectId) {
    DocumentEntity documentEntity = requireDocumentEntity(entity);
    String resolvedEntityId = resolveEntityId(user, documentEntity, entityId);
    FileStorageObject fso = fetchMetadataByEntityIdAndId(resolvedEntityId, fileStorageObjectId);

    checkAccess(user, entity, entityId, fso.getCategory(), OperationType.READ);

    try {
      InputStream documentStream = gcsService.getDocument(fso.getBlobId());
      fso.setUploadedFile(documentStream);
      return fso;
    } catch (Exception e) {
      logWarn(
          "Failed to retrieve file from GCS for fileStorageObjectId "
              + fileStorageObjectId
              + ": "
              + e.getMessage());
      throw new WebApplicationException(
          "Failed to retrieve file from storage", e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Lists all document metadata records for the given entity.
   *
   * <ol>
   *   <li>Calls {@link #checkAccess} with a null category (entity-level READ).
   *   <li>Resolves entity ID.
   *   <li>Fetches all FSO metadata.
   * </ol>
   */
  public List<FileStorageObject> listDocuments(User user, String entity, String entityId) {
    DocumentEntity documentEntity = requireDocumentEntity(entity);
    checkAccess(user, entity, entityId, null, OperationType.READ);

    String resolvedEntityId = resolveEntityId(user, documentEntity, entityId);
    return fetchAllMetadataByEntityId(resolvedEntityId);
  }

  /**
   * Updates the category of an existing document.
   *
   * <ol>
   *   <li>Parses and validates the new category for the entity.
   *   <li>Calls {@link #checkAccess} with the new category (WRITE).
   *   <li>Resolves entity ID, fetches FSO, updates category.
   * </ol>
   */
  public FileStorageObject updateDocumentCategory(
      User user, String entity, String entityId, Integer fileStorageObjectId, String categoryStr) {
    DocumentEntity documentEntity = requireDocumentEntity(entity);
    FileCategory category = requireValidCategory(categoryStr);
    validateCategoryForEntity(documentEntity, category);
    checkAccess(user, entity, entityId, category, OperationType.WRITE);

    String resolvedEntityId = resolveEntityId(user, documentEntity, entityId);
    FileStorageObject fileStorageObject =
        fetchMetadataByEntityIdAndId(resolvedEntityId, fileStorageObjectId);

    Instant updateDate = Instant.now();
    fileStorageObjectDAO.updateCategory(fileStorageObjectId, category.getValue(), user.getUserId());

    FileStorageObject updated = fileStorageObjectDAO.findById(fileStorageObjectId);
    if (updated != null) {
      return updated;
    }

    fileStorageObject.setCategory(category);
    fileStorageObject.setUpdateUserId(user.getUserId());
    fileStorageObject.setUpdateDate(updateDate);
    return fileStorageObject;
  }

  /**
   * Soft-deletes a document.
   *
   * <ol>
   *   <li>Resolves entity ID.
   *   <li>Fetches FSO metadata (including category).
   *   <li>Calls {@link #checkAccess} with the file's actual category (WRITE).
   *   <li>Soft-deletes the record.
   * </ol>
   */
  public FileStorageObject deleteDocument(
      User user, String entity, String entityId, Integer fileStorageObjectId) {
    DocumentEntity documentEntity = requireDocumentEntity(entity);
    String resolvedEntityId = resolveEntityId(user, documentEntity, entityId);
    FileStorageObject fileStorageObject =
        fetchMetadataByEntityIdAndId(resolvedEntityId, fileStorageObjectId);

    checkAccess(user, entity, entityId, fileStorageObject.getCategory(), OperationType.WRITE);

    Instant deleteDate = Instant.now();
    fileStorageObjectDAO.softDelete(resolvedEntityId, fileStorageObjectId, user.getUserId());

    FileStorageObject deleted = fileStorageObjectDAO.findById(fileStorageObjectId);
    if (deleted != null) {
      return deleted;
    }

    // Fallback: preserve response contract if DAO returns null
    logWarn(
        "Database did not return deleted FileStorageObject (id: "
            + fileStorageObjectId
            + ", entity: "
            + resolvedEntityId
            + "). Applying deletion flags in memory. "
            + "Next query for this file may show inconsistent state.");
    fileStorageObject.setDeleted(true);
    fileStorageObject.setDeleteUserId(user.getUserId());
    fileStorageObject.setDeleteDate(deleteDate);
    return fileStorageObject;
  }

  // ---------------------------------------------------------------------------
  // Centralized authorization
  // ---------------------------------------------------------------------------

  /**
   * Central authorization gate for all FileStorageObject operations.
   *
   * <p>Rules by entity / category / operation:
   *
   * <ul>
   *   <li><b>DAR</b> – {@link FileCategory#IRB_COLLABORATION_LETTER} and {@link
   *       FileCategory#DATA_USE_LETTER}:
   *       <ul>
   *         <li>WRITE: DAR creator only.
   *         <li>READ: ADMIN, CHAIRPERSON, MEMBER, or DAR creator.
   *       </ul>
   *   <li><b>DAC</b> – {@link FileCategory#DATA_ACCESS_AGREEMENT}:
   *       <ul>
   *         <li>WRITE: CHAIRPERSON of the specific DAC (entity-scoped) or ADMIN.
   *         <li>READ: any authenticated user.
   *       </ul>
   *   <li><b>DATASET</b> – {@link FileCategory#NIH_INSTITUTIONAL_CERTIFICATION}:
   *       <ul>
   *         <li>WRITE: ADMIN, DATASUBMITTER, or CHAIRPERSON.
   *         <li>READ: ADMIN, DATASUBMITTER, CHAIRPERSON, or MEMBER.
   *       </ul>
   *   <li><b>STUDY</b> – {@link FileCategory#ALTERNATIVE_DATA_SHARING_PLAN}:
   *       <ul>
   *         <li>WRITE: ADMIN, DATASUBMITTER, or CHAIRPERSON.
   *         <li>READ: ADMIN, DATASUBMITTER, or CHAIRPERSON (study must be DAC-linked; enforced by
   *             entity resolution via DatasetService).
   *       </ul>
   * </ul>
   *
   * <p>When {@code category} is {@code null} an entity-level read check is applied (used by list /
   * get metadata operations).
   *
   * @throws BadRequestException when (entity, category) combination is invalid.
   * @throws ForbiddenException when the user lacks the required permission.
   */
  public void checkAccess(
      User user, String entity, String entityId, FileCategory category, OperationType op) {
    DocumentEntity documentEntity = requireDocumentEntity(entity);

    if (category == null) {
      // Entity-level read: used by listDocuments / getDocument.
      checkEntityLevelReadAccess(user, documentEntity, entityId);
      return;
    }

    validateCategoryForEntity(documentEntity, category);

    switch (documentEntity) {
      case DAR -> checkDarAccess(user, entityId, op);
      case DAC -> checkDacAccess(user, entityId, op);
      case DATASET -> checkDatasetAccess(user, op);
      case STUDY -> checkStudyAccess(user);
    }
  }

  // ---------------------------------------------------------------------------
  // Per-entity access rules
  // ---------------------------------------------------------------------------

  private void checkEntityLevelReadAccess(User user, DocumentEntity entity, String entityId) {
    if (entity == DocumentEntity.DAC) {
      // All authenticated users may list DAC documents.
      return;
    }

    if (entity == DocumentEntity.DAR) {
      boolean isCreator = isDarCreator(user, entityId);
      if (!isCreator) {
        ensureHasAnyRole(user, UserRoles.ADMIN, UserRoles.CHAIRPERSON, UserRoles.MEMBER);
      }
      return;
    }

    if (entity == DocumentEntity.STUDY) {
      ensureHasAnyRole(user, UserRoles.ADMIN, UserRoles.DATASUBMITTER, UserRoles.CHAIRPERSON);
      return;
    }

    ensureHasAnyRole(
        user, UserRoles.ADMIN, UserRoles.DATASUBMITTER, UserRoles.CHAIRPERSON, UserRoles.MEMBER);
  }

  private void checkDarAccess(User user, String entityId, OperationType op) {
    boolean isCreator = isDarCreator(user, entityId);

    if (op == OperationType.WRITE && !isCreator) {
      throw new ForbiddenException(PERMISSION_DENIED);
    }

    if (op == OperationType.READ && !isCreator) {
      ensureHasAnyRole(user, UserRoles.ADMIN, UserRoles.CHAIRPERSON, UserRoles.MEMBER);
    }
  }

  private void checkDacAccess(User user, String entityId, OperationType op) {
    if (op == OperationType.WRITE) {
      Integer dacId = parseNumericEntityId(entityId);
      if (!isDacChair(user, dacId)) {
        throw new ForbiddenException(PERMISSION_DENIED);
      }
    }

    // READ: all authenticated users — no further check needed.
  }

  private void checkDatasetAccess(User user, OperationType op) {
    if (op == OperationType.WRITE) {
      ensureHasAnyRole(user, UserRoles.ADMIN, UserRoles.DATASUBMITTER, UserRoles.CHAIRPERSON);
      return;
    }

    ensureHasAnyRole(
        user, UserRoles.ADMIN, UserRoles.DATASUBMITTER, UserRoles.CHAIRPERSON, UserRoles.MEMBER);
  }

  private void checkStudyAccess(User user) {
    // Both READ and WRITE require ADMIN, DATASUBMITTER, or CHAIRPERSON.
    // For READ the study must be DAC-linked; that constraint is enforced during entity
    // resolution via DatasetService.findStudyByIdForRead.
    ensureHasAnyRole(user, UserRoles.ADMIN, UserRoles.DATASUBMITTER, UserRoles.CHAIRPERSON);
  }

  // ---------------------------------------------------------------------------
  // Authorization helper predicates
  // ---------------------------------------------------------------------------

  /** Returns {@code true} when the user is the creator of the given DAR. */
  boolean isDarCreator(User user, String darEntityId) {
    DataAccessRequest dar = dataAccessRequestService.findByReferenceId(darEntityId);
    return dar.getUserId() != null && dar.getUserId().equals(user.getUserId());
  }

  /**
   * Returns {@code true} when the user holds the CHAIRPERSON role scoped to the given DAC ID. ADMIN
   * users are treated as implicit chairs for all DACs.
   */
  boolean isDacChair(User user, Integer dacId) {
    if (hasRole(user, UserRoles.ADMIN)) {
      return true;
    }
    if (user.getRoles() == null) {
      return false;
    }
    return user.getRoles().stream()
        .filter(r -> Objects.equals(r.getRoleId(), UserRoles.CHAIRPERSON.getRoleId()))
        .map(UserRole::getDacId)
        .anyMatch(dacId::equals);
  }

  /** Returns {@code true} when the user has the specified global role. */
  boolean hasRole(User user, UserRoles role) {
    return user.hasUserRole(role);
  }

  private void ensureHasAnyRole(User user, UserRoles... roles) {
    for (UserRoles role : roles) {
      if (hasRole(user, role)) {
        return;
      }
    }
    throw new ForbiddenException(PERMISSION_DENIED);
  }

  // ---------------------------------------------------------------------------
  // Entity-ID resolution (pure — no auth for DAC / DAR)
  // ---------------------------------------------------------------------------

  private String resolveEntityId(User user, DocumentEntity entity, String entityId) {
    return switch (entity) {
      case DATASET -> resolveDatasetEntityId(user, entityId);
      case STUDY -> resolveStudyEntityId(user, entityId);
      case DAC -> resolveDacEntityId(entityId);
      case DAR -> entityId; // DAR uses the reference ID as-is
    };
  }

  private String resolveDatasetEntityId(User user, String entityId) {
    Integer datasetId = parseNumericEntityId(entityId);
    datasetService.findDatasetByIdForRead(user, datasetId);
    return datasetId.toString();
  }

  private String resolveStudyEntityId(User user, String entityId) {
    Integer studyId = parseNumericEntityId(entityId);
    Study study = datasetService.findStudyByIdForRead(user, studyId);
    if (study.getUuid() == null) {
      throw new NotFoundException(ENTITY_NOT_FOUND);
    }
    return study.getUuid().toString();
  }

  private String resolveDacEntityId(String entityId) {
    Integer dacId = parseNumericEntityId(entityId);
    dacService.findById(dacId);
    return dacId.toString();
  }

  // ---------------------------------------------------------------------------
  // Low-level GCS / DAO helpers
  // ---------------------------------------------------------------------------

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
      blobId = gcsService.storeDocument(content, mediaType, UUID.randomUUID());
    } catch (Exception e) {
      logWarn("Failed to upload file for user id " + createUserId + ": " + e.getMessage());
      throw e;
    }

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

  public FileStorageObject fetchById(Integer fileStorageObjectId) throws NotFoundException {
    FileStorageObject fileStorageObject = fileStorageObjectDAO.findFileById(fileStorageObjectId);
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

  public List<FileStorageObject> fetchAllMetadataByEntityId(String entityId) {
    return fileStorageObjectDAO.findFileMetadataByEntityId(entityId);
  }

  public List<FileStorageObject> fetchAllByEntityId(String entityId) throws NotFoundException {
    List<FileStorageObject> fileStorageObjects = fileStorageObjectDAO.findFilesByEntityId(entityId);
    fetchAndPopulateMultipleUploadedFiles(fileStorageObjects);
    return fileStorageObjects;
  }

  public List<FileStorageObject> fetchAllByEntityIdAndCategory(
      String entityId, FileCategory category) throws NotFoundException {
    List<FileStorageObject> fileStorageObjects =
        fileStorageObjectDAO.findFilesByEntityIdAndCategory(entityId, category.getValue());
    fetchAndPopulateMultipleUploadedFiles(fileStorageObjects);
    return fileStorageObjects;
  }

  private void fetchAndPopulateUploadedFile(FileStorageObject fileStorageObject) {
    try {
      InputStream document = gcsService.getDocument(fileStorageObject.getBlobId());
      fileStorageObject.setUploadedFile(document);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logWarn("Failed to get document from GCS: " + e.getMessage());
      throw e;
    }
  }

  private void fetchAndPopulateMultipleUploadedFiles(List<FileStorageObject> fileStorageObjects) {
    try {
      Map<BlobId, InputStream> documentMap =
          gcsService.getDocuments(
              fileStorageObjects.stream().map(FileStorageObject::getBlobId).toList());
      fileStorageObjects.forEach(fso -> fso.setUploadedFile(documentMap.get(fso.getBlobId())));
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logWarn("Failed to get document from GCS: " + e.getMessage());
      throw e;
    }
  }

  // ---------------------------------------------------------------------------
  // Validation helpers
  // ---------------------------------------------------------------------------

  private DocumentEntity requireDocumentEntity(String entity) {
    return DocumentEntity.fromValue(entity)
        .orElseThrow(() -> new NotFoundException(ENTITY_NOT_FOUND));
  }

  private FileCategory requireValidCategory(String categoryStr) {
    FileCategory category = FileCategory.findValue(categoryStr);
    if (category == null) {
      throw new BadRequestException("Invalid category");
    }
    return category;
  }

  private void validateCategoryForEntity(DocumentEntity entity, FileCategory category) {
    if (!isCategoryAllowedForEntity(entity, category)) {
      throw new BadRequestException("Category is not allowed for entity");
    }
  }

  private boolean isCategoryAllowedForEntity(DocumentEntity entity, FileCategory category) {
    return switch (entity) {
      case DAR ->
          category == FileCategory.IRB_COLLABORATION_LETTER
              || category == FileCategory.DATA_USE_LETTER;
      case DAC -> category == FileCategory.DATA_ACCESS_AGREEMENT;
      case DATASET -> category == FileCategory.NIH_INSTITUTIONAL_CERTIFICATION;
      case STUDY -> category == FileCategory.ALTERNATIVE_DATA_SHARING_PLAN;
    };
  }

  private Integer parseNumericEntityId(String entityId) {
    try {
      return Integer.valueOf(entityId);
    } catch (NumberFormatException _) {
      throw new NotFoundException(ENTITY_NOT_FOUND);
    }
  }
}
