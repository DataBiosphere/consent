package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.FileStorageObjectCategoryUpdateRequest;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.FileStorageObjectService;
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
    when(fileStorageObjectService.fetchAllMetadataByEntityAndEntityIdForRead(
            user, "dataset", Integer.toString(datasetId)))
        .thenReturn(files);

    try (var response =
        resource.findDocumentsByEntity(duosUser, "dataset", Integer.toString(datasetId))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(files, response.getEntity());
    }

    verify(fileStorageObjectService)
        .fetchAllMetadataByEntityAndEntityIdForRead(user, "dataset", Integer.toString(datasetId));
  }

  @Test
  void testFindDocumentsByDatasetEntityMixedCaseReturnsMetadata() {
    int datasetId = 123;
    List<FileStorageObject> files = List.of(new FileStorageObject());

    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.fetchAllMetadataByEntityAndEntityIdForRead(
            user, "DaTaSeT", Integer.toString(datasetId)))
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
    when(fileStorageObjectService.fetchAllMetadataByEntityAndEntityIdForRead(
            user, "study", Integer.toString(studyId)))
        .thenReturn(files);

    try (var response =
        resource.findDocumentsByEntity(duosUser, "study", Integer.toString(studyId))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(files, response.getEntity());
    }

    verify(fileStorageObjectService)
        .fetchAllMetadataByEntityAndEntityIdForRead(user, "study", Integer.toString(studyId));
  }

  @Test
  void testFindDocumentsByEntityNotFound() {
    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.fetchAllMetadataByEntityAndEntityIdForRead(
            user, "dataset", "111"))
        .thenThrow(new NotFoundException("Entity not found"));

    try (var response = resource.findDocumentsByEntity(duosUser, "dataset", "111")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testFindDocumentsByEntityForbidden() {
    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.fetchAllMetadataByEntityAndEntityIdForRead(
            user, "dataset", "222"))
        .thenThrow(new jakarta.ws.rs.ForbiddenException("User does not have permission"));

    try (var response = resource.findDocumentsByEntity(duosUser, "dataset", "222")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testFindDocumentsByStudyEntityReturnsEmptyList() {
    int studyId = 333;

    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.fetchAllMetadataByEntityAndEntityIdForRead(
            user, "study", Integer.toString(studyId)))
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
    when(fileStorageObjectService.fetchAllMetadataByEntityAndEntityIdForRead(
            user, "study", Integer.toString(studyId)))
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
    when(fileStorageObjectService.fetchMetadataByEntityAndEntityIdForRead(
            user, "dataset", Integer.toString(datasetId), fileId))
        .thenReturn(fileStorageObject);

    try (var response =
        resource.findDocumentByEntity(duosUser, "dataset", Integer.toString(datasetId), fileId)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(fileStorageObject, response.getEntity());
    }

    verify(fileStorageObjectService)
        .fetchMetadataByEntityAndEntityIdForRead(
            user, "dataset", Integer.toString(datasetId), fileId);
  }

  @Test
  void testFindDocumentByStudyEntityReturnsMetadata() {
    int studyId = 456;
    Integer fileId = 11;
    FileStorageObject fileStorageObject = new FileStorageObject();

    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.fetchMetadataByEntityAndEntityIdForRead(
            user, "study", Integer.toString(studyId), fileId))
        .thenReturn(fileStorageObject);

    try (var response =
        resource.findDocumentByEntity(duosUser, "study", Integer.toString(studyId), fileId)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(fileStorageObject, response.getEntity());
    }

    verify(fileStorageObjectService)
        .fetchMetadataByEntityAndEntityIdForRead(user, "study", Integer.toString(studyId), fileId);
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
    when(fileStorageObjectService.fetchMetadataByEntityAndEntityIdForRead(
            user, "dataset", "123", 10))
        .thenThrow(new NotFoundException("Entity not found"));

    try (var response = resource.findDocumentByEntity(duosUser, "dataset", "123", 10)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testFindDocumentByEntityForbiddenWhenNoReadAccess() {
    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.fetchMetadataByEntityAndEntityIdForRead(
            user, "dataset", "123", 10))
        .thenThrow(new jakarta.ws.rs.ForbiddenException("User does not have permission"));

    try (var response = resource.findDocumentByEntity(duosUser, "dataset", "123", 10)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @ParameterizedTest(name = "Not found when metadata lookup fails: {1}")
  @CsvSource({"10,file not found", "11,file deleted", "12,file belongs to different entity"})
  void testFindDocumentByEntityNotFoundWhenFileLookupFails(
      Integer fileId, String scenarioDescription) {
    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.fetchMetadataByEntityAndEntityIdForRead(
            user, "dataset", "123", fileId))
        .thenThrow(new NotFoundException("File not found"));

    try (var response = resource.findDocumentByEntity(duosUser, "dataset", "123", fileId)) {
      assertEquals(
          HttpStatusCodes.STATUS_CODE_NOT_FOUND,
          response.getStatus(),
          "Expected 404 for scenario: " + scenarioDescription);
    }
  }

  @Test
  void testUpdateDocumentCategoryByEntitySuccess() {
    FileStorageObjectCategoryUpdateRequest request = new FileStorageObjectCategoryUpdateRequest();
    request.setCategory("dataUseLetter");
    FileStorageObject updated = new FileStorageObject();

    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.updateCategoryByEntityAndEntityIdForWrite(
            user, "dataset", "123", 10, "dataUseLetter"))
        .thenReturn(updated);

    try (var response =
        resource.updateDocumentCategoryByEntity(duosUser, "dataset", "123", 10, request)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(updated, response.getEntity());
    }
  }

  @Test
  void testUpdateDocumentCategoryByEntityInvalidCategoryBadRequest() {
    FileStorageObjectCategoryUpdateRequest request = new FileStorageObjectCategoryUpdateRequest();
    request.setCategory("invalidCategory");

    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.updateCategoryByEntityAndEntityIdForWrite(
            user, "dataset", "123", 10, "invalidCategory"))
        .thenThrow(new BadRequestException("Invalid category"));

    try (var response =
        resource.updateDocumentCategoryByEntity(duosUser, "dataset", "123", 10, request)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testUpdateDocumentCategoryByEntityInvalidCategoryEntityMappingBadRequest() {
    FileStorageObjectCategoryUpdateRequest request = new FileStorageObjectCategoryUpdateRequest();
    request.setCategory("dataAccessAgreement");

    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.updateCategoryByEntityAndEntityIdForWrite(
            user, "dataset", "123", 10, "dataAccessAgreement"))
        .thenThrow(new BadRequestException("Category not allowed"));

    try (var response =
        resource.updateDocumentCategoryByEntity(duosUser, "dataset", "123", 10, request)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testUpdateDocumentCategoryByEntityNotFound() {
    FileStorageObjectCategoryUpdateRequest request = new FileStorageObjectCategoryUpdateRequest();
    request.setCategory("dataUseLetter");

    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.updateCategoryByEntityAndEntityIdForWrite(
            user, "dataset", "123", 10, "dataUseLetter"))
        .thenThrow(new NotFoundException("File not found"));

    try (var response =
        resource.updateDocumentCategoryByEntity(duosUser, "dataset", "123", 10, request)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testUpdateDocumentCategoryByEntityForbidden() {
    FileStorageObjectCategoryUpdateRequest request = new FileStorageObjectCategoryUpdateRequest();
    request.setCategory("dataUseLetter");

    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.updateCategoryByEntityAndEntityIdForWrite(
            user, "dataset", "123", 10, "dataUseLetter"))
        .thenThrow(new jakarta.ws.rs.ForbiddenException("User does not have permission"));

    try (var response =
        resource.updateDocumentCategoryByEntity(duosUser, "dataset", "123", 10, request)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testUpdateDocumentCategoryByEntityNullRequestCategoryBadRequest() {
    when(duosUser.getUser()).thenReturn(user);
    when(fileStorageObjectService.updateCategoryByEntityAndEntityIdForWrite(
            user, "dataset", "123", 10, null))
        .thenThrow(new BadRequestException("Invalid category"));

    try (var response =
        resource.updateDocumentCategoryByEntity(
            duosUser, "dataset", "123", 10, new FileStorageObjectCategoryUpdateRequest())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }
}
