package org.broadinstitute.consent.http.service;

import static org.broadinstitute.consent.http.AbstractTestHelper.nextInt;
import static org.broadinstitute.consent.http.AbstractTestHelper.randomAlphabetic;
import static org.broadinstitute.consent.http.AbstractTestHelper.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.BlobId;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.FileStorageObjectDAO;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.User;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileStorageObjectServiceTest {

  @Mock private Jdbi jdbi;
  @Mock private FileStorageObjectDAO fileStorageObjectDAO;

  @Mock private GCSService gcsService;
  @Mock private DatasetService datasetService;
  @Mock private DacService dacService;
  @Mock private DaaService daaService;
  @Mock private DataAccessRequestService dataAccessRequestService;

  private FileStorageObjectService service;

  @BeforeEach
  void setUp() {
    when(jdbi.onDemand(FileStorageObjectDAO.class)).thenReturn(fileStorageObjectDAO);
    service =
        new FileStorageObjectService(
            jdbi, gcsService, datasetService, dacService, daaService, dataAccessRequestService);
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

    FileStorageObject returned = service.fetchMetadataByEntityIdAndId(entityId, fileId);

    assertEquals(fileStorageObject, returned);
    verify(fileStorageObjectDAO).findActiveFileByIdAndEntityId(entityId, fileId);
  }

  @ParameterizedTest
  @ValueSource(ints = {10, 11, 12})
  void testFetchMetadataByIdForEntityNotFoundWhenFileMissingDeletedOrWrongEntity(Integer fileId) {
    String entityId = randomAlphabetic(10);

    when(fileStorageObjectDAO.findActiveFileByIdAndEntityId(entityId, fileId)).thenReturn(null);

    assertThrows(
        NotFoundException.class, () -> service.fetchMetadataByEntityIdAndId(entityId, fileId));
  }

  @Test
  void testFetchMetadataByEntityAndEntityIdForReadDataset() {
    User user = new User();
    user.setMemberRole();
    Integer datasetId = 123;
    Integer fileId = 10;
    FileStorageObject fileStorageObject = new FileStorageObject();

    when(datasetService.findDatasetByIdForRead(user, datasetId)).thenReturn(new Dataset());
    when(fileStorageObjectDAO.findActiveFileByIdAndEntityIdAndCategories(
            datasetId.toString(),
            fileId,
            List.of(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION.getValue())))
        .thenReturn(fileStorageObject);

    FileStorageObject returned = service.getDocument(user, "dataset", datasetId.toString(), fileId);

    assertEquals(fileStorageObject, returned);
  }

  @Test
  void testAllFetchMetadataByEntityAndEntityIdForReadStudy() {
    User user = new User();
    user.setChairpersonRole();
    Integer studyId = 456;
    Study study = new Study();
    study.setUuid(java.util.UUID.randomUUID());
    List<FileStorageObject> fileStorageObjects = List.of(new FileStorageObject());

    when(datasetService.findStudyByIdForRead(user, studyId)).thenReturn(study);
    when(fileStorageObjectDAO.findFileMetadataByEntityIdAndCategories(
            study.getUuid().toString(),
            List.of(FileCategory.ALTERNATIVE_DATA_SHARING_PLAN.getValue())))
        .thenReturn(fileStorageObjects);

    List<FileStorageObject> returnedFiles =
        service.listDocuments(user, "study", studyId.toString());

    assertEquals(fileStorageObjects, returnedFiles);
  }

  @ParameterizedTest
  @CsvSource({"MEMBER,false", "CHAIRPERSON,true"})
  void testListDocumentsForStudyAccessByRole(String role, boolean allowed) {
    User user = new User();
    applyRole(user, role);
    Integer studyId = 456;
    Study study = new Study();
    study.setUuid(java.util.UUID.randomUUID());
    String studyEntityId = studyId.toString();
    List<FileStorageObject> fileStorageObjects = List.of(new FileStorageObject());

    if (!allowed) {
      assertThrows(
          ForbiddenException.class, () -> service.listDocuments(user, "study", studyEntityId));
      verifyNoInteractions(datasetService);
      verifyNoInteractions(fileStorageObjectDAO);
      return;
    }

    when(datasetService.findStudyByIdForRead(user, studyId)).thenReturn(study);
    when(fileStorageObjectDAO.findFileMetadataByEntityIdAndCategories(
            study.getUuid().toString(),
            List.of(FileCategory.ALTERNATIVE_DATA_SHARING_PLAN.getValue())))
        .thenReturn(fileStorageObjects);

    List<FileStorageObject> returnedFiles =
        service.listDocuments(user, "study", studyId.toString());

    assertEquals(fileStorageObjects, returnedFiles);
    verify(fileStorageObjectDAO)
        .findFileMetadataByEntityIdAndCategories(
            study.getUuid().toString(),
            List.of(FileCategory.ALTERNATIVE_DATA_SHARING_PLAN.getValue()));
  }

  private void applyRole(User user, String role) {
    switch (role) {
      case "MEMBER" -> user.setMemberRole();
      case "CHAIRPERSON" -> user.setChairpersonRole();
      default -> throw new IllegalArgumentException("Unsupported role: " + role);
    }
  }

  @Test
  void testFetchAllMetadataByEntityAndEntityIdForReadDac() {
    User user = new User();
    Integer dacId = 456;
    Integer daaId = 789;
    List<FileStorageObject> fileStorageObjects = List.of(new FileStorageObject());

    when(dacService.findById(dacId)).thenReturn(new Dac());
    when(daaService.findDaaIdsByDacId(dacId)).thenReturn(List.of(daaId));
    when(fileStorageObjectDAO.findFileMetadataByEntityIdAndCategories(
            daaId.toString(), List.of(FileCategory.DATA_ACCESS_AGREEMENT.getValue())))
        .thenReturn(fileStorageObjects);

    List<FileStorageObject> returnedFiles = service.listDocuments(user, "dac", dacId.toString());

    assertEquals(fileStorageObjects, returnedFiles);
    verify(fileStorageObjectDAO)
        .findFileMetadataByEntityIdAndCategories(
            daaId.toString(), List.of(FileCategory.DATA_ACCESS_AGREEMENT.getValue()));
  }

  @Test
  void testUploadDocumentForDacStoresUsingDaaId() throws Exception {
    InputStream inputStream = new ByteArrayInputStream("metadata".getBytes());
    FormDataContentDisposition fileDetail =
        FormDataContentDisposition.name("file").fileName("upload.pdf").size(32).build();

    User user = new User();
    user.setUserId(25);
    user.setChairpersonRoleWithDAC(123);

    FileStorageObject created = new FileStorageObject();
    created.setFileStorageObjectId(90);

    when(daaService.createAndLinkDaaIdForDac(user, 123)).thenReturn(456);
    when(gcsService.storeDocument(eq(inputStream), eq("application/octet-stream"), any()))
        .thenReturn(BlobId.of("bucket", "object"));
    when(fileStorageObjectDAO.insertNewFile(
            eq("upload.pdf"),
            eq(FileCategory.DATA_ACCESS_AGREEMENT.getValue()),
            eq(BlobId.of("bucket", "object").toGsUtilUri()),
            eq("application/octet-stream"),
            eq("456"),
            eq(25),
            any()))
        .thenReturn(90);
    when(fileStorageObjectDAO.findFileById(90)).thenReturn(created);

    FileStorageObject result =
        service.uploadDocument(user, "dac", "123", inputStream, fileDetail, "dataAccessAgreement");

    assertEquals(created, result);
    verify(fileStorageObjectDAO)
        .insertNewFile(
            eq("upload.pdf"),
            eq(FileCategory.DATA_ACCESS_AGREEMENT.getValue()),
            eq(BlobId.of("bucket", "object").toGsUtilUri()),
            eq("application/octet-stream"),
            eq("456"),
            eq(25),
            any());
  }

  @Test
  void testGetDocumentForDacFailsWhenFileDaaNotLinkedToDac() {
    User user = new User();
    user.setAdminRole();
    Integer dacId = 123;
    Integer fileId = 10;

    FileStorageObject fso = new FileStorageObject();
    fso.setFileStorageObjectId(fileId);
    fso.setEntityId("999");
    fso.setCategory(FileCategory.DATA_ACCESS_AGREEMENT);
    fso.setDeleted(false);

    when(dacService.findById(dacId)).thenReturn(new Dac());
    when(fileStorageObjectDAO.findFileById(fileId)).thenReturn(fso);
    when(daaService.isDaaLinkedToDac(dacId, 999)).thenReturn(false);

    assertThrows(NotFoundException.class, () -> service.getDocument(user, "dac", "123", fileId));
  }

  @Test
  void testListDocumentsForDacAggregatesAcrossLinkedDaaIds() {
    User user = new User();
    user.setAdminRole();
    Integer dacId = 456;

    FileStorageObject daa1File = new FileStorageObject();
    FileStorageObject daa2File = new FileStorageObject();

    when(dacService.findById(dacId)).thenReturn(new Dac());
    when(daaService.findDaaIdsByDacId(dacId)).thenReturn(List.of(1001, 1002));
    when(fileStorageObjectDAO.findFileMetadataByEntityIdAndCategories(
            "1001", List.of(FileCategory.DATA_ACCESS_AGREEMENT.getValue())))
        .thenReturn(List.of(daa1File));
    when(fileStorageObjectDAO.findFileMetadataByEntityIdAndCategories(
            "1002", List.of(FileCategory.DATA_ACCESS_AGREEMENT.getValue())))
        .thenReturn(List.of(daa2File));

    List<FileStorageObject> returnedFiles = service.listDocuments(user, "dac", dacId.toString());

    assertEquals(2, returnedFiles.size());
    assertEquals(List.of(daa1File, daa2File), returnedFiles);
  }

  @Test
  void testListDocumentsForDacReturnsEmptyWhenNoLinkedDaaIds() {
    User user = new User();
    user.setAdminRole();
    Integer dacId = 456;

    when(dacService.findById(dacId)).thenReturn(new Dac());
    when(daaService.findDaaIdsByDacId(dacId)).thenReturn(List.of());

    List<FileStorageObject> returnedFiles = service.listDocuments(user, "dac", dacId.toString());

    assertTrue(returnedFiles.isEmpty());
    verifyNoInteractions(fileStorageObjectDAO);
  }

  @Test
  void testGetDocumentFileForDacFailsWhenFileDaaNotLinkedToDac() {
    User user = new User();
    user.setAdminRole();
    Integer dacId = 123;
    Integer fileId = 10;

    FileStorageObject fso = new FileStorageObject();
    fso.setFileStorageObjectId(fileId);
    fso.setEntityId("999");
    fso.setCategory(FileCategory.DATA_ACCESS_AGREEMENT);
    fso.setDeleted(false);

    when(dacService.findById(dacId)).thenReturn(new Dac());
    when(fileStorageObjectDAO.findFileById(fileId)).thenReturn(fso);
    when(daaService.isDaaLinkedToDac(dacId, 999)).thenReturn(false);

    assertThrows(
        NotFoundException.class, () -> service.getDocumentFile(user, "dac", "123", fileId));
    verifyNoInteractions(gcsService);
  }

  @Test
  void testUpdateDocumentCategoryForDacFailsWhenFileDaaNotLinkedToDac() {
    User user = new User();
    user.setUserId(25);
    Integer dacId = 123;
    user.setChairpersonRoleWithDAC(dacId);
    Integer fileId = 10;

    FileStorageObject fso = new FileStorageObject();
    fso.setFileStorageObjectId(fileId);
    fso.setEntityId("999");
    fso.setCategory(FileCategory.DATA_ACCESS_AGREEMENT);
    fso.setDeleted(false);

    when(dacService.findById(dacId)).thenReturn(new Dac());
    when(fileStorageObjectDAO.findFileById(fileId)).thenReturn(fso);
    when(daaService.isDaaLinkedToDac(dacId, 999)).thenReturn(false);
    String category = FileCategory.DATA_ACCESS_AGREEMENT.getValue();

    assertThrows(
        NotFoundException.class,
        () -> service.updateDocumentCategory(user, "dac", "123", fileId, category));
    verify(fileStorageObjectDAO, never()).updateCategory(any(), any(), any());
  }

  @Test
  void testDeleteDocumentForDacFailsWhenFileDaaNotLinkedToDac() {
    User user = new User();
    user.setUserId(25);
    Integer dacId = 123;
    user.setChairpersonRoleWithDAC(dacId);
    Integer fileId = 10;

    FileStorageObject fso = new FileStorageObject();
    fso.setFileStorageObjectId(fileId);
    fso.setEntityId("999");
    fso.setCategory(FileCategory.DATA_ACCESS_AGREEMENT);
    fso.setDeleted(false);

    when(dacService.findById(dacId)).thenReturn(new Dac());
    when(fileStorageObjectDAO.findFileById(fileId)).thenReturn(fso);
    when(daaService.isDaaLinkedToDac(dacId, 999)).thenReturn(false);

    assertThrows(NotFoundException.class, () -> service.deleteDocument(user, "dac", "123", fileId));
    verify(fileStorageObjectDAO, never()).softDelete(any(), any(), any());
  }

  @Test
  void testDeleteDocumentForDacUsesDaaEntityIdWhenLinked() {
    User user = new User();
    user.setUserId(25);
    Integer dacId = 123;
    user.setChairpersonRoleWithDAC(dacId);
    Integer fileId = 10;

    FileStorageObject active = new FileStorageObject();
    active.setFileStorageObjectId(fileId);
    active.setEntityId("777");
    active.setCategory(FileCategory.DATA_ACCESS_AGREEMENT);
    active.setDeleted(false);

    FileStorageObject deleted = new FileStorageObject();
    deleted.setFileStorageObjectId(fileId);
    deleted.setEntityId("777");
    deleted.setDeleted(true);

    when(dacService.findById(dacId)).thenReturn(new Dac());
    when(fileStorageObjectDAO.findFileById(fileId)).thenReturn(active);
    when(daaService.isDaaLinkedToDac(dacId, 777)).thenReturn(true);
    when(fileStorageObjectDAO.findById(fileId)).thenReturn(deleted);

    FileStorageObject result = service.deleteDocument(user, "dac", "123", fileId);

    assertEquals(deleted, result);
    verify(fileStorageObjectDAO).softDelete("777", fileId, user.getUserId());
  }

  @Test
  void testFetchAllMetadataByEntityAndEntityIdForReadDar() {
    User user = new User();
    user.setUserId(25);
    String darReferenceId = "DAR-123";
    List<FileStorageObject> fileStorageObjects = List.of(new FileStorageObject());
    DataAccessRequest dar = new DataAccessRequest();
    dar.setUserId(user.getUserId());

    when(dataAccessRequestService.findByReferenceId(darReferenceId)).thenReturn(dar);
    when(fileStorageObjectDAO.findFileMetadataByEntityIdAndCategories(
            darReferenceId,
            List.of(
                FileCategory.IRB_COLLABORATION_LETTER.getValue(),
                FileCategory.DATA_USE_LETTER.getValue())))
        .thenReturn(fileStorageObjects);

    List<FileStorageObject> returnedFiles = service.listDocuments(user, "dar", darReferenceId);

    assertEquals(fileStorageObjects, returnedFiles);
    verify(fileStorageObjectDAO)
        .findFileMetadataByEntityIdAndCategories(
            darReferenceId,
            List.of(
                FileCategory.IRB_COLLABORATION_LETTER.getValue(),
                FileCategory.DATA_USE_LETTER.getValue()));
  }

  @Test
  void testFetchMetadataByEntityAndEntityIdForReadStudy() {
    User user = new User();
    user.setChairpersonRole();
    Integer studyId = 456;
    Integer fileId = 11;
    Study study = new Study();
    study.setUuid(java.util.UUID.randomUUID());
    FileStorageObject fileStorageObject = new FileStorageObject();

    when(datasetService.findStudyByIdForRead(user, studyId)).thenReturn(study);
    when(fileStorageObjectDAO.findActiveFileByIdAndEntityIdAndCategories(
            study.getUuid().toString(),
            fileId,
            List.of(FileCategory.ALTERNATIVE_DATA_SHARING_PLAN.getValue())))
        .thenReturn(fileStorageObject);

    FileStorageObject returned = service.getDocument(user, "study", studyId.toString(), fileId);

    assertEquals(fileStorageObject, returned);
  }

  @Test
  void testUploadDocumentForDataset() throws Exception {
    InputStream inputStream = new ByteArrayInputStream("metadata".getBytes());
    FormDataContentDisposition fileDetail =
        FormDataContentDisposition.name("file").fileName("upload.pdf").size(32).build();

    User user = new User();
    user.setUserId(25);
    user.setRoles(List.of(UserRoles.DataSubmitter()));

    Dataset dataset = new Dataset();
    FileStorageObject created = new FileStorageObject();
    created.setFileStorageObjectId(90);

    when(datasetService.findDatasetByIdForRead(user, 123)).thenReturn(dataset);
    when(gcsService.storeDocument(eq(inputStream), eq("application/octet-stream"), any()))
        .thenReturn(BlobId.of("bucket", "object"));
    when(fileStorageObjectDAO.insertNewFile(
            eq("upload.pdf"),
            eq(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION.getValue()),
            eq(BlobId.of("bucket", "object").toGsUtilUri()),
            eq("application/octet-stream"),
            eq("123"),
            eq(25),
            any()))
        .thenReturn(90);
    when(fileStorageObjectDAO.findFileById(90)).thenReturn(created);

    FileStorageObject result =
        service.uploadDocument(
            user, "dataset", "123", inputStream, fileDetail, "nihInstitutionalCertification");

    assertEquals(created, result);
    verify(datasetService).findDatasetByIdForRead(user, 123);
    verify(fileStorageObjectDAO)
        .insertNewFile(
            eq("upload.pdf"),
            eq(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION.getValue()),
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

    assertThrows(
        BadRequestException.class,
        () ->
            service.uploadDocument(
                user, "dataset", "123", inputStream, fileDetail, "notARealCategory"));

    verifyNoInteractions(fileStorageObjectDAO);
    verifyNoInteractions(gcsService);
  }

  @Test
  void testGetDocumentFileByEntityAndEntityIdForReadDataset() throws Exception {
    User user = new User();
    user.setMemberRole();
    Integer datasetId = 123;
    Integer fileId = 10;

    FileStorageObject fileStorageObject = new FileStorageObject();
    fileStorageObject.setBlobId(BlobId.of("bucket", "document"));
    fileStorageObject.setCategory(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION);

    byte[] content = "streamed-file-content".getBytes();

    when(datasetService.findDatasetByIdForRead(user, datasetId)).thenReturn(new Dataset());
    when(fileStorageObjectDAO.findActiveFileByIdAndEntityIdAndCategories(
            datasetId.toString(),
            fileId,
            List.of(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION.getValue())))
        .thenReturn(fileStorageObject);
    when(gcsService.getDocument(fileStorageObject.getBlobId()))
        .thenReturn(new ByteArrayInputStream(content));

    FileStorageObject returned =
        service.getDocumentFile(user, "dataset", datasetId.toString(), fileId);

    assertEquals(fileStorageObject, returned);
    assertArrayEquals(content, returned.getUploadedFile().readAllBytes());
    verify(fileStorageObjectDAO)
        .findActiveFileByIdAndEntityIdAndCategories(
            datasetId.toString(),
            fileId,
            List.of(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION.getValue()));
    verify(gcsService).getDocument(fileStorageObject.getBlobId());
  }

  @Test
  void testGetDocumentFileThrowsBadGatewayWhenGcsFails() {
    User user = new User();
    user.setMemberRole();
    Integer datasetId = 123;
    Integer fileId = 10;
    String entityId = datasetId.toString();

    FileStorageObject fileStorageObject = new FileStorageObject();
    fileStorageObject.setBlobId(BlobId.of("bucket", "document"));
    fileStorageObject.setCategory(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION);

    when(datasetService.findDatasetByIdForRead(user, datasetId)).thenReturn(new Dataset());
    when(fileStorageObjectDAO.findActiveFileByIdAndEntityIdAndCategories(
            entityId, fileId, List.of(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION.getValue())))
        .thenReturn(fileStorageObject);
    when(gcsService.getDocument(fileStorageObject.getBlobId()))
        .thenThrow(new RuntimeException("GCS unavailable"));

    WebApplicationException exception =
        assertThrows(
            WebApplicationException.class,
            () -> service.getDocumentFile(user, "dataset", entityId, fileId));
    assertEquals(500, exception.getResponse().getStatus());
  }

  @Test
  void testGetDocumentFileThrowsNotFoundWhenMetadataLookupFails() {
    User user = new User();
    // No role needed here — NotFoundException is thrown before checkAccess (null FSO)
    Integer datasetId = 123;
    Integer fileId = 10;
    String entityId = datasetId.toString();

    when(datasetService.findDatasetByIdForRead(user, datasetId)).thenReturn(new Dataset());
    when(fileStorageObjectDAO.findActiveFileByIdAndEntityIdAndCategories(
            entityId, fileId, List.of(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION.getValue())))
        .thenReturn(null);

    assertThrows(
        NotFoundException.class, () -> service.getDocumentFile(user, "dataset", entityId, fileId));
    verifyNoInteractions(gcsService);
  }

  @Test
  void testDeleteDocumentSetsDeletedFieldsAndCallsDao() {
    User user = new User();
    user.setUserId(25);
    user.setRoles(List.of(UserRoles.DataSubmitter()));
    Integer datasetId = 123;
    Integer fileId = 10;
    String entityId = datasetId.toString();

    FileStorageObject active = new FileStorageObject();
    active.setFileStorageObjectId(fileId);
    active.setEntityId(entityId);
    active.setDeleted(false);
    active.setCategory(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION);

    FileStorageObject deleted = new FileStorageObject();
    deleted.setFileStorageObjectId(fileId);
    deleted.setEntityId(entityId);
    deleted.setDeleted(true);
    deleted.setDeleteUserId(user.getUserId());
    deleted.setDeleteDate(java.time.Instant.now());

    when(datasetService.findDatasetByIdForRead(user, datasetId)).thenReturn(new Dataset());
    when(fileStorageObjectDAO.findActiveFileByIdAndEntityIdAndCategories(
            entityId, fileId, List.of(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION.getValue())))
        .thenReturn(active);
    when(fileStorageObjectDAO.findById(fileId)).thenReturn(deleted);

    FileStorageObject result = service.deleteDocument(user, "dataset", entityId, fileId);

    assertEquals(Boolean.TRUE, result.getDeleted());
    assertEquals(user.getUserId(), result.getDeleteUserId());
    assertNotNull(result.getDeleteDate());
    verify(fileStorageObjectDAO).softDelete(entityId, fileId, user.getUserId());
  }

  @Test
  void testDeleteDocumentThrowsNotFoundWhenFileAlreadyDeleted() {
    User user = new User();
    user.setUserId(25);
    Integer datasetId = 123;
    Integer fileId = 10;
    String entityId = datasetId.toString();

    when(datasetService.findDatasetByIdForRead(user, datasetId)).thenReturn(new Dataset());
    when(fileStorageObjectDAO.findActiveFileByIdAndEntityIdAndCategories(
            entityId, fileId, List.of(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION.getValue())))
        .thenReturn(null);

    NotFoundException exception =
        assertThrows(
            NotFoundException.class,
            () -> service.deleteDocument(user, "dataset", entityId, fileId));

    assertEquals("File not found", exception.getMessage());
    verify(fileStorageObjectDAO, never()).softDelete(any(), any(), any());
    verify(fileStorageObjectDAO, never()).findById(any());
  }

  @Test
  void testUpdateDocumentCategoryUpdatesCategoryAndAuditFields() {
    User user = new User();
    user.setUserId(25);
    user.setRoles(List.of(UserRoles.DataSubmitter()));
    Integer datasetId = 123;
    Integer fileId = 10;
    String entityId = datasetId.toString();

    FileStorageObject active = new FileStorageObject();
    active.setFileStorageObjectId(fileId);
    active.setEntityId(entityId);
    active.setCategory(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION);
    active.setDeleted(false);

    FileStorageObject updated = new FileStorageObject();
    updated.setFileStorageObjectId(fileId);
    updated.setEntityId(entityId);
    updated.setCategory(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION);
    updated.setUpdateUserId(user.getUserId());
    updated.setUpdateDate(java.time.Instant.now());

    when(datasetService.findDatasetByIdForRead(user, datasetId)).thenReturn(new Dataset());
    when(fileStorageObjectDAO.findActiveFileByIdAndEntityIdAndCategories(
            entityId, fileId, List.of(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION.getValue())))
        .thenReturn(active);
    when(fileStorageObjectDAO.findById(fileId)).thenReturn(updated);

    FileStorageObject result =
        service.updateDocumentCategory(
            user, "dataset", entityId, fileId, "nihInstitutionalCertification");

    assertEquals(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION, result.getCategory());
    assertEquals(user.getUserId(), result.getUpdateUserId());
    assertNotNull(result.getUpdateDate());
    verify(fileStorageObjectDAO)
        .updateCategory(fileId, "nihInstitutionalCertification", user.getUserId());
  }

  @Test
  void testUpdateDocumentCategoryThrowsBadRequestWhenCategoryInvalid() {
    User user = new User();

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> service.updateDocumentCategory(user, "dataset", "123", 10, "notARealCategory"));

    assertEquals("Invalid category", exception.getMessage());
    verifyNoInteractions(fileStorageObjectDAO);
    verifyNoInteractions(datasetService);
  }

  @Test
  void testUpdateDocumentCategoryThrowsNotFoundWhenFileDeletedOrMissing() {
    User user = new User();
    user.setUserId(25);
    user.setRoles(List.of(UserRoles.DataSubmitter()));
    Integer datasetId = 123;
    Integer fileId = 10;
    String entityId = datasetId.toString();

    when(datasetService.findDatasetByIdForRead(user, datasetId)).thenReturn(new Dataset());
    when(fileStorageObjectDAO.findActiveFileByIdAndEntityIdAndCategories(
            entityId, fileId, List.of(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION.getValue())))
        .thenReturn(null);

    NotFoundException exception =
        assertThrows(
            NotFoundException.class,
            () ->
                service.updateDocumentCategory(
                    user, "dataset", entityId, fileId, "nihInstitutionalCertification"));

    assertEquals("File not found", exception.getMessage());
    verify(fileStorageObjectDAO, never()).updateCategory(any(), any(), any());
    verify(fileStorageObjectDAO, never()).findById(any());
  }

  @Test
  void testUpdateDocumentCategoryThrowsBadRequestWhenCategoryNotAllowedForEntity() {
    User user = new User();
    user.setUserId(25);
    int datasetId = 123;
    String entityId = Integer.toString(datasetId);

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () ->
                service.updateDocumentCategory(
                    user, "dataset", entityId, 10, "dataAccessAgreement"));

    assertEquals("Category is not allowed for entity", exception.getMessage());
    verifyNoInteractions(fileStorageObjectDAO);
    verifyNoInteractions(datasetService);
  }

  @Test
  void testListDocumentsAllowedForAdminRead() {
    User user = new User();
    user.setAdminRole();
    String entityId = "123";
    List<FileStorageObject> fileStorageObjects = List.of(new FileStorageObject());

    when(datasetService.findDatasetByIdForRead(user, Integer.valueOf(entityId)))
        .thenReturn(new Dataset());
    when(fileStorageObjectDAO.findFileMetadataByEntityIdAndCategories(
            entityId, List.of(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION.getValue())))
        .thenReturn(fileStorageObjects);

    List<FileStorageObject> returned = service.listDocuments(user, "dataset", entityId);

    assertEquals(fileStorageObjects, returned);
    verify(datasetService).findDatasetByIdForRead(user, Integer.valueOf(entityId));
    verify(fileStorageObjectDAO)
        .findFileMetadataByEntityIdAndCategories(
            entityId, List.of(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION.getValue()));
  }

  @Test
  void testUploadDocumentForbiddenForAdmin() {
    InputStream inputStream = new ByteArrayInputStream("metadata".getBytes());
    FormDataContentDisposition fileDetail =
        FormDataContentDisposition.name("file").fileName("upload.pdf").size(32).build();
    User user = new User();
    user.setAdminRole();

    assertThrows(
        ForbiddenException.class,
        () ->
            service.uploadDocument(
                user, "dataset", "123", inputStream, fileDetail, "nihInstitutionalCertification"));
    verifyNoInteractions(datasetService);
    verifyNoInteractions(fileStorageObjectDAO);
    verifyNoInteractions(gcsService);
  }
}
