package org.broadinstitute.consent.http.service;

import static org.broadinstitute.consent.http.AbstractTestHelper.nextInt;
import static org.broadinstitute.consent.http.AbstractTestHelper.randomAlphabetic;
import static org.broadinstitute.consent.http.AbstractTestHelper.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.BlobId;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.FileStorageObjectDAO;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileStorageObjectServiceTest {

  @Mock private FileStorageObjectDAO fileStorageObjectDAO;

  @Mock private GCSService gcsService;
  @Mock private DatasetService datasetService;

  private FileStorageObjectService service;

  private void initService() {
    service = new FileStorageObjectService(fileStorageObjectDAO, gcsService, datasetService);
  }

  @Test
  void testUploadAndStoreFile() throws IOException {
    InputStream content = new ByteArrayInputStream(randomAlphanumeric(20).getBytes());
    String fileName = randomAlphabetic(10);
    String mediaType = randomAlphabetic(10);
    FileCategory category =
        List.of(FileCategory.values())
            .get(
                org.broadinstitute.consent.http.AbstractTestHelper.randomInt(
                    0, FileCategory.values().length));
    String entityId = randomAlphabetic(10);
    Integer createUserId = nextInt();

    String bucket = randomAlphabetic(10);
    String blob = randomAlphabetic(10);

    when(fileStorageObjectDAO.insertNewFile(
            eq(fileName),
            eq(category.getValue()),
            eq(BlobId.of(bucket, blob).toGsUtilUri()),
            eq(mediaType),
            eq(entityId),
            eq(createUserId),
            any()))
        .thenReturn(10);

    FileStorageObject newFileStorageObject = new FileStorageObject();
    newFileStorageObject.setFileName(randomAlphabetic(10));

    when(fileStorageObjectDAO.findFileById(10)).thenReturn(newFileStorageObject);
    when(gcsService.storeDocument(eq(content), eq(mediaType), any()))
        .thenReturn(BlobId.of(bucket, blob));

    initService();

    FileStorageObject returned =
        service.uploadAndStoreFile(content, fileName, mediaType, category, entityId, createUserId);

    assertEquals(newFileStorageObject, returned);

    verify(gcsService, times(1)).storeDocument(eq(content), eq(mediaType), any());

    verify(fileStorageObjectDAO, times(1))
        .insertNewFile(
            eq(fileName),
            eq(category.getValue()),
            eq(BlobId.of(bucket, blob).toGsUtilUri()),
            eq(mediaType),
            eq(entityId),
            eq(createUserId),
            any());
  }

  @Test
  void testFetchById() throws IOException {
    String bucket = randomAlphabetic(10);
    String blob = randomAlphabetic(10);

    FileStorageObject file = new FileStorageObject();
    file.setBlobId(BlobId.of(bucket, blob));

    String content = randomAlphanumeric(100);

    when(gcsService.getDocument(BlobId.of(bucket, blob)))
        .thenReturn(new ByteArrayInputStream(content.getBytes()));

    when(fileStorageObjectDAO.findFileById(10)).thenReturn(file);

    initService();

    FileStorageObject returned = service.fetchById(10);

    assertEquals(file, returned);

    assertArrayEquals(content.getBytes(), returned.getUploadedFile().readAllBytes());
  }

  @Test
  void testFetchAllByEntityId() throws IOException {
    String bucket1Name = randomAlphabetic(10);
    String blob1Name = randomAlphabetic(10);
    String bucket2Name = randomAlphabetic(10);
    String blob2Name = randomAlphabetic(10);
    String bucket3Name = randomAlphabetic(10);
    String blob3Name = randomAlphabetic(10);

    FileStorageObject file1 = new FileStorageObject();
    file1.setBlobId(BlobId.of(bucket1Name, blob1Name));

    FileStorageObject file2 = new FileStorageObject();
    file2.setBlobId(BlobId.of(bucket2Name, blob2Name));

    FileStorageObject file3 = new FileStorageObject();
    file3.setBlobId(BlobId.of(bucket3Name, blob3Name));

    String content1 = randomAlphabetic(10);
    String content2 = randomAlphabetic(10);
    String content3 = randomAlphabetic(10);

    when(gcsService.getDocuments(List.of(file1.getBlobId(), file2.getBlobId(), file3.getBlobId())))
        .thenReturn(
            Map.of(
                file1.getBlobId(), new ByteArrayInputStream(content1.getBytes()),
                file2.getBlobId(), new ByteArrayInputStream(content2.getBytes()),
                file3.getBlobId(), new ByteArrayInputStream(content3.getBytes())));

    String entityId = randomAlphabetic(10);

    when(fileStorageObjectDAO.findFilesByEntityId(entityId))
        .thenReturn(List.of(file1, file2, file3));

    initService();

    List<FileStorageObject> returned = service.fetchAllByEntityId(entityId);

    assertEquals(3, returned.size());

    assertEquals(file1, returned.get(0));
    assertEquals(file2, returned.get(1));
    assertEquals(file3, returned.get(2));

    assertArrayEquals(content1.getBytes(), returned.get(0).getUploadedFile().readAllBytes());
    assertArrayEquals(content2.getBytes(), returned.get(1).getUploadedFile().readAllBytes());
    assertArrayEquals(content3.getBytes(), returned.get(2).getUploadedFile().readAllBytes());
  }

  @Test
  void testFetchAllByEntityIdAndCategory() throws IOException {
    String bucket1Name = randomAlphabetic(10);
    String blob1Name = randomAlphabetic(10);
    String bucket2Name = randomAlphabetic(10);
    String blob2Name = randomAlphabetic(10);
    String bucket3Name = randomAlphabetic(10);
    String blob3Name = randomAlphabetic(10);

    FileStorageObject file1 = new FileStorageObject();
    file1.setBlobId(BlobId.of(bucket1Name, blob1Name));

    FileStorageObject file2 = new FileStorageObject();
    file2.setBlobId(BlobId.of(bucket2Name, blob2Name));

    FileStorageObject file3 = new FileStorageObject();
    file3.setBlobId(BlobId.of(bucket3Name, blob3Name));

    String content1 = randomAlphabetic(10);
    String content2 = randomAlphabetic(10);
    String content3 = randomAlphabetic(10);

    when(gcsService.getDocuments(List.of(file1.getBlobId(), file2.getBlobId(), file3.getBlobId())))
        .thenReturn(
            Map.of(
                file1.getBlobId(), new ByteArrayInputStream(content1.getBytes()),
                file2.getBlobId(), new ByteArrayInputStream(content2.getBytes()),
                file3.getBlobId(), new ByteArrayInputStream(content3.getBytes())));

    String entityId = randomAlphabetic(10);
    FileCategory category =
        List.of(FileCategory.values())
            .get(
                org.broadinstitute.consent.http.AbstractTestHelper.randomInt(
                    0, FileCategory.values().length));

    when(fileStorageObjectDAO.findFilesByEntityIdAndCategory(entityId, category.getValue()))
        .thenReturn(List.of(file1, file2, file3));

    initService();

    List<FileStorageObject> returned = service.fetchAllByEntityIdAndCategory(entityId, category);

    assertEquals(3, returned.size());

    assertEquals(file1, returned.get(0));
    assertEquals(file2, returned.get(1));
    assertEquals(file3, returned.get(2));

    assertArrayEquals(content1.getBytes(), returned.get(0).getUploadedFile().readAllBytes());
    assertArrayEquals(content2.getBytes(), returned.get(1).getUploadedFile().readAllBytes());
    assertArrayEquals(content3.getBytes(), returned.get(2).getUploadedFile().readAllBytes());
  }

  @Test
  void testFetchAllMetadataByEntityId() {
    String entityId = RandomStringUtils.secure().nextAlphabetic(10);
    FileStorageObject file1 = new FileStorageObject();
    FileStorageObject file2 = new FileStorageObject();
    List<FileStorageObject> expected = List.of(file1, file2);

    when(fileStorageObjectDAO.findFileMetadataByEntityId(entityId)).thenReturn(expected);

    initService();

    List<FileStorageObject> returned = service.fetchAllMetadataByEntityId(entityId);

    assertEquals(expected, returned);
    verify(fileStorageObjectDAO).findFileMetadataByEntityId(entityId);
    verifyNoInteractions(gcsService);
  }

  @Test
  void testFetchMetadataByIdForEntity() {
    Integer fileId = 10;
    String entityId = randomAlphabetic(10);
    FileStorageObject fileStorageObject = new FileStorageObject();

    when(fileStorageObjectDAO.findActiveFileByIdAndEntityId(entityId, fileId))
        .thenReturn(fileStorageObject);

    initService();

    FileStorageObject returned = service.fetchMetadataByEntityIdAndId(entityId, fileId);

    assertEquals(fileStorageObject, returned);
    verify(fileStorageObjectDAO).findActiveFileByIdAndEntityId(entityId, fileId);
  }

  @ParameterizedTest
  @ValueSource(ints = {10, 11, 12})
  void testFetchMetadataByIdForEntityNotFoundWhenFileMissingDeletedOrWrongEntity(Integer fileId) {
    String entityId = randomAlphabetic(10);

    when(fileStorageObjectDAO.findActiveFileByIdAndEntityId(entityId, fileId)).thenReturn(null);

    initService();

    assertThrows(
        NotFoundException.class, () -> service.fetchMetadataByEntityIdAndId(entityId, fileId));
  }

  @Test
  void testFetchMetadataByEntityAndEntityIdForReadDataset() {
    User user = new User();
    Integer datasetId = 123;
    Integer fileId = 10;
    FileStorageObject fileStorageObject = new FileStorageObject();

    when(datasetService.findDatasetByIdForRead(user, datasetId)).thenReturn(new Dataset());
    when(fileStorageObjectDAO.findActiveFileByIdAndEntityId(datasetId.toString(), fileId))
        .thenReturn(fileStorageObject);

    initService();

    FileStorageObject returned =
        service.fetchMetadataByEntityAndEntityIdForRead(
            user, "dataset", datasetId.toString(), fileId);

    assertEquals(fileStorageObject, returned);
  }

  @Test
  void testAllFetchMetadataByEntityAndEntityIdForReadStudy() {
    User user = new User();
    Integer studyId = 456;
    Study study = new Study();
    study.setUuid(java.util.UUID.randomUUID());
    List<FileStorageObject> fileStorageObjects = List.of(new FileStorageObject());

    when(datasetService.findStudyByIdForRead(user, studyId)).thenReturn(study);
    when(fileStorageObjectDAO.findFileMetadataByEntityId(study.getUuid().toString()))
        .thenReturn(fileStorageObjects);

    initService();

    List<FileStorageObject> returnedFiles =
        service.fetchAllMetadataByEntityAndEntityIdForRead(user, "study", studyId.toString());

    assertEquals(fileStorageObjects, returnedFiles);
  }

  @Test
  void testFetchMetadataByEntityAndEntityIdForReadStudy() {
    User user = new User();
    Integer studyId = 456;
    Integer fileId = 11;
    Study study = new Study();
    study.setUuid(java.util.UUID.randomUUID());
    FileStorageObject fileStorageObject = new FileStorageObject();

    when(datasetService.findStudyByIdForRead(user, studyId)).thenReturn(study);
    when(fileStorageObjectDAO.findActiveFileByIdAndEntityId(study.getUuid().toString(), fileId))
        .thenReturn(fileStorageObject);

    initService();

    FileStorageObject returned =
        service.fetchMetadataByEntityAndEntityIdForRead(user, "study", studyId.toString(), fileId);

    assertEquals(fileStorageObject, returned);
  }

  @Test
  void testUpdateCategoryByEntityAndEntityIdForWriteSuccess() {
    User user = new User();
    user.setUserId(200);
    Integer datasetId = 123;
    Integer fileId = 10;
    String entityId = datasetId.toString();

    Dataset dataset = new Dataset();
    dataset.setCreateUserId(user.getUserId());
    FileStorageObject existing = new FileStorageObject();
    existing.setDeleted(false);
    FileStorageObject updated = new FileStorageObject();
    updated.setCategory(FileCategory.DATA_USE_LETTER);

    when(datasetService.findDatasetByIdForRead(user, datasetId)).thenReturn(dataset);
    when(fileStorageObjectDAO.findFileByIdAndEntityId(entityId, fileId)).thenReturn(existing);
    when(fileStorageObjectDAO.updateCategory(
            eq(fileId), eq("dataUseLetter"), eq(user.getUserId()), any()))
        .thenReturn(1);
    when(fileStorageObjectDAO.findFileById(fileId)).thenReturn(updated);

    initService();

    FileStorageObject result =
        service.updateCategoryByEntityAndEntityIdForWrite(
            user, "dataset", entityId, fileId, "dataUseLetter");

    assertEquals(updated, result);
    verify(fileStorageObjectDAO)
        .updateCategory(eq(fileId), eq("dataUseLetter"), eq(user.getUserId()), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"invalidCategory", "dataAccessAgreement"})
  void testUpdateCategoryByEntityAndEntityIdForWriteBadRequestForCategory(String category) {
    User user = new User();
    user.setUserId(201);

    initService();

    String entityId = "123";
    assertThrows(
        BadRequestException.class,
        () ->
            service.updateCategoryByEntityAndEntityIdForWrite(
                user, "dataset", entityId, 10, category));
  }

  @Test
  void testUpdateCategoryByEntityAndEntityIdForWriteNotFoundWhenMissingFile() {
    User user = new User();
    user.setUserId(203);
    Integer datasetId = 123;
    Integer fileId = 10;
    String entityId = datasetId.toString();
    Dataset dataset = new Dataset();
    dataset.setCreateUserId(user.getUserId());

    when(datasetService.findDatasetByIdForRead(user, datasetId)).thenReturn(dataset);
    when(fileStorageObjectDAO.findFileByIdAndEntityId(entityId, fileId)).thenReturn(null);

    initService();

    assertThrows(
        NotFoundException.class,
        () ->
            service.updateCategoryByEntityAndEntityIdForWrite(
                user, "dataset", entityId, fileId, "dataUseLetter"));
  }

  @Test
  void testUpdateCategoryByEntityAndEntityIdForWriteNotFoundWhenDeletedFile() {
    User user = new User();
    user.setUserId(204);
    Integer datasetId = 123;
    Integer fileId = 10;
    String entityId = datasetId.toString();
    Dataset dataset = new Dataset();
    dataset.setCreateUserId(user.getUserId());
    FileStorageObject deleted = new FileStorageObject();
    deleted.setDeleted(true);

    when(datasetService.findDatasetByIdForRead(user, datasetId)).thenReturn(dataset);
    when(fileStorageObjectDAO.findFileByIdAndEntityId(entityId, fileId)).thenReturn(deleted);

    initService();

    assertThrows(
        NotFoundException.class,
        () ->
            service.updateCategoryByEntityAndEntityIdForWrite(
                user, "dataset", entityId, fileId, "dataUseLetter"));
  }

  @Test
  void testUpdateCategoryByEntityAndEntityIdForWriteForbidden() {
    User user = new User();
    user.setUserId(205);
    Integer datasetId = 123;
    String entityId = datasetId.toString();
    Dataset dataset = new Dataset();
    dataset.setCreateUserId(999);

    when(datasetService.findDatasetByIdForRead(user, datasetId)).thenReturn(dataset);

    initService();

    assertThrows(
        ForbiddenException.class,
        () ->
            service.updateCategoryByEntityAndEntityIdForWrite(
                user, "dataset", entityId, 10, "dataUseLetter"));
  }

  @Test
  void testUpdateCategoryByEntityAndEntityIdForWriteInvalidEntityType() {
    User user = new User();
    user.setUserId(206);

    initService();

    assertThrows(
        NotFoundException.class,
        () ->
            service.updateCategoryByEntityAndEntityIdForWrite(
                user, "invalid", "123", 10, "dataUseLetter"));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "   "})
  void testUpdateCategoryByEntityAndEntityIdForWriteNullOrEmptyCategory(String category) {
    User user = new User();
    user.setUserId(207);

    initService();

    assertThrows(
        BadRequestException.class,
        () ->
            service.updateCategoryByEntityAndEntityIdForWrite(
                user, "dataset", "123", 10, category));
  }

  @Test
  void testUpdateCategoryByEntityAndEntityIdForWriteSameCategoryStillSucceeds() {
    User user = new User();
    user.setUserId(208);
    Integer datasetId = 123;
    Integer fileId = 10;
    Dataset dataset = new Dataset();
    dataset.setCreateUserId(user.getUserId());
    FileStorageObject existing = new FileStorageObject();
    existing.setDeleted(false);
    existing.setCategory(FileCategory.DATA_USE_LETTER);

    when(datasetService.findDatasetByIdForRead(user, datasetId)).thenReturn(dataset);
    when(fileStorageObjectDAO.findFileByIdAndEntityId(datasetId.toString(), fileId))
        .thenReturn(existing);
    when(fileStorageObjectDAO.updateCategory(
            eq(fileId), eq("dataUseLetter"), eq(user.getUserId()), any()))
        .thenReturn(1);
    when(fileStorageObjectDAO.findFileById(fileId)).thenReturn(existing);

    initService();

    FileStorageObject result =
        service.updateCategoryByEntityAndEntityIdForWrite(
            user, "dataset", datasetId.toString(), fileId, "dataUseLetter");

    assertEquals(existing, result);
  }
}
