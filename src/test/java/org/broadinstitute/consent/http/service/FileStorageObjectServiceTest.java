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
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileStorageObjectServiceTest {

  @Mock private FileStorageObjectDAO fileStorageObjectDAO;

  @Mock private GCSService gcsService;
  @Mock private DatasetService datasetService;
  @Mock private DacService dacService;
  @Mock private DataAccessRequestService dataAccessRequestService;

  private FileStorageObjectService service;

  private void initService() {
    service =
        new FileStorageObjectService(
            fileStorageObjectDAO, gcsService, datasetService, dacService, dataAccessRequestService);
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

    when(fileStorageObjectDAO.create(
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
        .create(
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
  void testUploadDocumentForDataset() throws Exception {
    InputStream inputStream = new ByteArrayInputStream("metadata".getBytes());
    FormDataContentDisposition fileDetail =
        FormDataContentDisposition.name("file").fileName("upload.pdf").size(32).build();

    User user = new User();
    user.setUserId(25);

    Dataset dataset = new Dataset();
    FileStorageObject created = new FileStorageObject();
    created.setFileStorageObjectId(90);

    when(datasetService.findDatasetByIdForRead(user, 123)).thenReturn(dataset);
    when(gcsService.storeDocument(eq(inputStream), eq("application/octet-stream"), any()))
        .thenReturn(BlobId.of("bucket", "object"));
    when(fileStorageObjectDAO.create(
            eq("upload.pdf"),
            eq(FileCategory.DATA_USE_LETTER.getValue()),
            eq(BlobId.of("bucket", "object").toGsUtilUri()),
            eq("application/octet-stream"),
            eq("123"),
            eq(25),
            any()))
        .thenReturn(90);
    when(fileStorageObjectDAO.findFileById(90)).thenReturn(created);

    initService();

    FileStorageObject result =
        service.uploadDocument(user, "dataset", "123", inputStream, fileDetail, "dataUseLetter");

    assertEquals(created, result);
    verify(datasetService).findDatasetByIdForRead(user, 123);
    verify(fileStorageObjectDAO)
        .create(
            eq("upload.pdf"),
            eq(FileCategory.DATA_USE_LETTER.getValue()),
            eq(BlobId.of("bucket", "object").toGsUtilUri()),
            eq("application/octet-stream"),
            eq("123"),
            eq(25),
            any());
  }

  @Test
  void testUploadDocumentInvalidCategoryThrowsBadRequest() {
    InputStream inputStream = new ByteArrayInputStream("metadata".getBytes());
    FormDataContentDisposition fileDetail =
        FormDataContentDisposition.name("file").fileName("upload.pdf").size(32).build();
    User user = new User();

    initService();

    assertThrows(
        BadRequestException.class,
        () ->
            service.uploadDocument(
                user, "dataset", "123", inputStream, fileDetail, "notARealCategory"));

    verifyNoInteractions(fileStorageObjectDAO);
    verifyNoInteractions(gcsService);
  }
}
