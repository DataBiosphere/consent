package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.NotFoundException;
import java.util.UUID;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.DatasetService;
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

  @Mock private DatasetService datasetService;
  @Mock private FileStorageObjectService fileStorageObjectService;
  @Mock private DuosUser duosUser;
  @Mock private User user;

  private DocumentResource resource;

  @BeforeEach
  void setUp() {
    resource = new DocumentResource(datasetService, fileStorageObjectService);
  }

  @Test
  void testFindDocumentByDatasetEntityReturnsMetadata() {
    Integer datasetId = 123;
    Integer fileId = 10;
    FileStorageObject fileStorageObject = new FileStorageObject();

    when(duosUser.getUser()).thenReturn(user);
    when(datasetService.findDatasetByIdForRead(user, datasetId)).thenReturn(new Dataset());
    when(fileStorageObjectService.fetchMetadataByEntityIdAndId(datasetId.toString(), fileId))
        .thenReturn(fileStorageObject);

    try (var response =
        resource.findDocumentByEntity(duosUser, "dataset", datasetId.toString(), fileId)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(fileStorageObject, response.getEntity());
    }

    verify(fileStorageObjectService).fetchMetadataByEntityIdAndId(datasetId.toString(), fileId);
  }

  @Test
  void testFindDocumentByStudyEntityReturnsMetadata() {
    Integer studyId = 456;
    Integer fileId = 11;
    Study study = new Study();
    study.setUuid(UUID.randomUUID());
    study.setPublicVisibility(true);
    FileStorageObject fileStorageObject = new FileStorageObject();

    when(duosUser.getUser()).thenReturn(user);
    when(datasetService.findStudy(studyId)).thenReturn(study);
    when(fileStorageObjectService.fetchMetadataByEntityIdAndId(study.getUuid().toString(), fileId))
        .thenReturn(fileStorageObject);

    try (var response =
        resource.findDocumentByEntity(duosUser, "study", studyId.toString(), fileId)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(fileStorageObject, response.getEntity());
    }

    verify(fileStorageObjectService)
        .fetchMetadataByEntityIdAndId(study.getUuid().toString(), fileId);
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
    when(datasetService.findDatasetByIdForRead(user, 123))
        .thenThrow(new NotFoundException("Entity not found"));

    try (var response = resource.findDocumentByEntity(duosUser, "dataset", "123", 10)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testFindDocumentByEntityForbiddenWhenNoReadAccess() {
    when(duosUser.getUser()).thenReturn(user);
    when(datasetService.findDatasetByIdForRead(user, 123))
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
    when(datasetService.findDatasetByIdForRead(user, 123)).thenReturn(new Dataset());
    when(fileStorageObjectService.fetchMetadataByEntityIdAndId("123", fileId))
        .thenThrow(new NotFoundException("File not found"));

    try (var response = resource.findDocumentByEntity(duosUser, "dataset", "123", fileId)) {
      assertEquals(
          HttpStatusCodes.STATUS_CODE_NOT_FOUND,
          response.getStatus(),
          "Expected 404 for scenario: " + scenarioDescription);
    }
  }
}
