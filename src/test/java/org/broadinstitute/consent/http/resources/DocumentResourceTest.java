package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.FileStorageObjectCategoryUpdateRequest;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.FileStorageObjectService;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentResourceTest {

  @Mock private FileStorageObjectService fileStorageObjectService;
  @Mock private DuosUser duosUser;
  @Mock private User user;
  @Mock private FormDataContentDisposition fileDetail;

  private DocumentResource resource;

  @BeforeEach
  void setUp() {
    resource = new DocumentResource(fileStorageObjectService);
  }

  @Test
  void testFindDocumentsByDatasetEntityReturnsMetadata() {
    int datasetId = 123;
    List<FileStorageObject> files = List.of(new FileStorageObject());

    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.listDocuments(user, "dataset", Integer.toString(datasetId)))
        .thenReturn(files);

    try (var response =
        resource.findDocumentsByEntity(duosUser, "dataset", Integer.toString(datasetId))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(files, response.getEntity());
    }

    verify(fileStorageObjectService).listDocuments(user, "dataset", Integer.toString(datasetId));
  }

  @Test
  void testFindDocumentsByDatasetEntityMixedCaseReturnsMetadata() {
    int datasetId = 123;
    List<FileStorageObject> files = List.of(new FileStorageObject());

    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.listDocuments(user, "DaTaSeT", Integer.toString(datasetId)))
        .thenReturn(files);

    try (var response =
        resource.findDocumentsByEntity(duosUser, "DaTaSeT", Integer.toString(datasetId))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(files, response.getEntity());
    }
  }

  @Test
  void testFindDocumentsByStudyEntityReturnsMetadata() {
    int studyId = 456;
    List<FileStorageObject> files = List.of(new FileStorageObject());

    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.listDocuments(user, "study", Integer.toString(studyId)))
        .thenReturn(files);

    try (var response =
        resource.findDocumentsByEntity(duosUser, "study", Integer.toString(studyId))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(files, response.getEntity());
    }

    verify(fileStorageObjectService).listDocuments(user, "study", Integer.toString(studyId));
  }

  @Test
  void testFindDocumentsByDacEntityReturnsMetadata() {
    List<FileStorageObject> files = List.of(new FileStorageObject());

    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.listDocuments(user, "dac", "456")).thenReturn(files);

    try (var response = resource.findDocumentsByEntity(duosUser, "dac", "456")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(files, response.getEntity());
    }

    verify(fileStorageObjectService).listDocuments(user, "dac", "456");
  }

  @Test
  void testFindDocumentsByDarEntityReturnsMetadata() {
    List<FileStorageObject> files = List.of(new FileStorageObject());

    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.listDocuments(user, "dar", "DAR-123")).thenReturn(files);

    try (var response = resource.findDocumentsByEntity(duosUser, "dar", "DAR-123")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(files, response.getEntity());
    }

    verify(fileStorageObjectService).listDocuments(user, "dar", "DAR-123");
  }

  @Test
  void testFindDocumentsByEntityNotFound() {
    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.listDocuments(user, "dataset", "111"))
        .thenThrow(new NotFoundException("Entity not found"));

    try (var response = resource.findDocumentsByEntity(duosUser, "dataset", "111")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testFindDocumentsByEntityForbidden() {
    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.listDocuments(user, "dataset", "222"))
        .thenThrow(new jakarta.ws.rs.ForbiddenException("User does not have permission"));

    try (var response = resource.findDocumentsByEntity(duosUser, "dataset", "222")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testFindDocumentsByStudyEntityReturnsEmptyList() {
    int studyId = 333;

    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.listDocuments(user, "study", Integer.toString(studyId)))
        .thenReturn(List.of());

    try (var response =
        resource.findDocumentsByEntity(duosUser, "study", Integer.toString(studyId))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(List.of(), response.getEntity());
    }
  }

  @Test
  void testFindDocumentsByStudyEntityForbidden() {
    int studyId = 444;

    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.listDocuments(user, "study", Integer.toString(studyId)))
        .thenThrow(new jakarta.ws.rs.ForbiddenException("User does not have permission"));

    try (var response =
        resource.findDocumentsByEntity(duosUser, "study", Integer.toString(studyId))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testFindDocumentByDatasetEntityReturnsMetadata() {
    int datasetId = 123;
    Integer fileId = 10;
    FileStorageObject fileStorageObject = new FileStorageObject();

    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.getDocument(user, "dataset", Integer.toString(datasetId), fileId))
        .thenReturn(fileStorageObject);

    try (var response =
        resource.findDocumentByEntity(duosUser, "dataset", Integer.toString(datasetId), fileId)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(fileStorageObject, response.getEntity());
    }

    verify(fileStorageObjectService)
        .getDocument(user, "dataset", Integer.toString(datasetId), fileId);
  }

  @Test
  void testFindDocumentByStudyEntityReturnsMetadata() {
    int studyId = 456;
    Integer fileId = 11;
    FileStorageObject fileStorageObject = new FileStorageObject();

    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.getDocument(user, "study", Integer.toString(studyId), fileId))
        .thenReturn(fileStorageObject);

    try (var response =
        resource.findDocumentByEntity(duosUser, "study", Integer.toString(studyId), fileId)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(fileStorageObject, response.getEntity());
    }

    verify(fileStorageObjectService).getDocument(user, "study", Integer.toString(studyId), fileId);
  }

  @Test
  void testFindDocumentByEntityUnauthenticatedForbidden() {
    try (var response = resource.findDocumentByEntity(null, "dataset", "123", 10)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
    }
  }

  @Test
  void testFindDocumentByEntityNotFoundWhenEntityMissing() {
    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.getDocument(user, "dataset", "123", 10))
        .thenThrow(new NotFoundException("Entity not found"));

    try (var response = resource.findDocumentByEntity(duosUser, "dataset", "123", 10)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testFindDocumentByEntityForbiddenWhenNoReadAccess() {
    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.getDocument(user, "dataset", "123", 10))
        .thenThrow(new jakarta.ws.rs.ForbiddenException("User does not have permission"));

    try (var response = resource.findDocumentByEntity(duosUser, "dataset", "123", 10)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testDeleteDocumentByEntityReturnsUpdatedMetadata() {
    FileStorageObject deleted = new FileStorageObject();
    deleted.setDeleted(true);

    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.deleteDocument(user, "dataset", "123", 10)).thenReturn(deleted);

    try (var response = resource.deleteDocumentByEntity(duosUser, "dataset", "123", 10)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(deleted, response.getEntity());
    }

    verify(fileStorageObjectService).deleteDocument(user, "dataset", "123", 10);
  }

  @Test
  void testDeleteDocumentByEntityNotFound() {
    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.deleteDocument(user, "dataset", "123", 10))
        .thenThrow(new NotFoundException("File not found"));

    try (var response = resource.deleteDocumentByEntity(duosUser, "dataset", "123", 10)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testDeleteDocumentByEntityForbidden() {
    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.deleteDocument(user, "dataset", "123", 10))
        .thenThrow(new jakarta.ws.rs.ForbiddenException("User does not have permission"));

    try (var response = resource.deleteDocumentByEntity(duosUser, "dataset", "123", 10)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testUpdateDocumentCategoryByEntityReturnsUpdatedMetadata() {
    FileStorageObjectCategoryUpdateRequest request = new FileStorageObjectCategoryUpdateRequest();
    request.setCategory("dataUseLetter");

    FileStorageObject updated = new FileStorageObject();

    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.updateDocumentCategory(
            user, "dataset", "123", 10, "dataUseLetter"))
        .thenReturn(updated);

    try (var response =
        resource.updateDocumentCategoryByEntity(duosUser, "dataset", "123", 10, request)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(updated, response.getEntity());
    }

    verify(fileStorageObjectService)
        .updateDocumentCategory(user, "dataset", "123", 10, "dataUseLetter");
  }

  @Test
  void testUpdateDocumentCategoryByEntityInvalidCategoryReturnsBadRequest() {
    FileStorageObjectCategoryUpdateRequest request = new FileStorageObjectCategoryUpdateRequest();
    request.setCategory("badCategory");

    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.updateDocumentCategory(user, "dataset", "123", 10, "badCategory"))
        .thenThrow(new jakarta.ws.rs.BadRequestException("Invalid category"));

    try (var response =
        resource.updateDocumentCategoryByEntity(duosUser, "dataset", "123", 10, request)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testUpdateDocumentCategoryByEntityNotFoundReturnsNotFound() {
    FileStorageObjectCategoryUpdateRequest request = new FileStorageObjectCategoryUpdateRequest();
    request.setCategory("dataUseLetter");

    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.updateDocumentCategory(
            user, "dataset", "123", 10, "dataUseLetter"))
        .thenThrow(new NotFoundException("File not found"));

    try (var response =
        resource.updateDocumentCategoryByEntity(duosUser, "dataset", "123", 10, request)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testUpdateDocumentCategoryByEntityCategoryEntityMismatchReturnsBadRequest() {
    FileStorageObjectCategoryUpdateRequest request = new FileStorageObjectCategoryUpdateRequest();
    request.setCategory("dataAccessAgreement");

    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.updateDocumentCategory(
            user, "dataset", "123", 10, "dataAccessAgreement"))
        .thenThrow(new jakarta.ws.rs.BadRequestException("Category is not allowed for entity"));

    try (var response =
        resource.updateDocumentCategoryByEntity(duosUser, "dataset", "123", 10, request)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @ParameterizedTest(name = "Not found when metadata lookup fails: {1}")
  @CsvSource({"10,file not found", "11,file deleted", "12,file belongs to different entity"})
  void testFindDocumentByEntityNotFoundWhenFileLookupFails(
      Integer fileId, String scenarioDescription) {
    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.getDocument(user, "dataset", "123", fileId))
        .thenThrow(new NotFoundException("File not found"));

    try (var response = resource.findDocumentByEntity(duosUser, "dataset", "123", fileId)) {
      assertEquals(
          HttpStatusCodes.STATUS_CODE_NOT_FOUND,
          response.getStatus(),
          "Expected 404 for scenario: " + scenarioDescription);
    }
  }

  @Test
  void testUploadDocumentReturnsCreated() throws Exception {
    ByteArrayInputStream inputStream = new ByteArrayInputStream("file-content".getBytes());
    FileStorageObject created = new FileStorageObject();

    when(fileDetail.getFileName()).thenReturn("document.pdf");
    when(fileDetail.getSize()).thenReturn(1024L);
    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.uploadDocument(
            user, "dataset", "123", inputStream, fileDetail, "dataUseLetter"))
        .thenReturn(created);

    try (var response =
        resource.uploadDocument(
            duosUser, "dataset", "123", inputStream, fileDetail, "dataUseLetter")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_CREATED, response.getStatus());
      assertEquals(created, response.getEntity());
    }

    verify(fileStorageObjectService)
        .uploadDocument(user, "dataset", "123", inputStream, fileDetail, "dataUseLetter");
  }

  @Test
  void testUploadDocumentInvalidCategoryReturnsBadRequest() throws Exception {
    ByteArrayInputStream inputStream = new ByteArrayInputStream("file-content".getBytes());

    when(fileDetail.getFileName()).thenReturn("document.pdf");
    when(fileDetail.getSize()).thenReturn(1024L);
    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.uploadDocument(
            user, "dataset", "123", inputStream, fileDetail, "badCategory"))
        .thenThrow(new jakarta.ws.rs.BadRequestException("Invalid category"));

    try (var response =
        resource.uploadDocument(
            duosUser, "dataset", "123", inputStream, fileDetail, "badCategory")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testFindDocumentFileByEntityReturnsStreamAndHeaders() throws Exception {
    byte[] content = "test-file-content".getBytes();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(content);

    FileStorageObject fileStorageObject = new FileStorageObject();
    fileStorageObject.setFileName("document.pdf");
    fileStorageObject.setMediaType("application/pdf");
    fileStorageObject.setUploadedFile(inputStream);

    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.getDocumentFile(user, "dataset", "123", 10))
        .thenReturn(fileStorageObject);

    try (var response = resource.findDocumentFileByEntity(duosUser, "dataset", "123", 10)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals("application/pdf", response.getHeaderString("Content-Type"));
      assertEquals(
          "attachment; filename=\"document.pdf\"", response.getHeaderString("Content-Disposition"));

      StreamingOutput streamingOutput = (StreamingOutput) response.getEntity();
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      streamingOutput.write(output);
      assertArrayEquals(content, output.toByteArray());
    }

    verify(fileStorageObjectService).getDocumentFile(user, "dataset", "123", 10);
  }

  @Test
  void testFindDocumentFileByEntityNotFound() {
    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.getDocumentFile(user, "dataset", "123", 10))
        .thenThrow(new NotFoundException("File not found"));

    try (var response = resource.findDocumentFileByEntity(duosUser, "dataset", "123", 10)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testFindDocumentFileByEntityReturnsInternalServerErrorWhenStorageFails() {
    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.getDocumentFile(user, "dataset", "123", 10))
        .thenThrow(
            new WebApplicationException(
                "Failed to retrieve file from storage", Response.Status.INTERNAL_SERVER_ERROR));

    try (var response = resource.findDocumentFileByEntity(duosUser, "dataset", "123", 10)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
    }
  }
}
