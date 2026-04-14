package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import java.util.List;
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
  void testFindDocumentsByDatasetEntityReturnsMetadata() {
    Integer datasetId = 123;
    List<FileStorageObject> files = List.of(new FileStorageObject());

    when(duosUser.getUser()).thenReturn(user);
    when(datasetService.findDatasetByIdForRead(user, datasetId)).thenReturn(new Dataset());
    when(fileStorageObjectService.fetchAllMetadataByEntityId(datasetId.toString()))
        .thenReturn(files);

    try (var response = resource.findDocumentsByEntity(duosUser, "dataset", datasetId.toString())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(files, response.getEntity());
    }

    verify(fileStorageObjectService).fetchAllMetadataByEntityId(datasetId.toString());
  }

  @Test
  void testFindDocumentsByDatasetEntityMixedCaseReturnsMetadata() {
    Integer datasetId = 123;
    List<FileStorageObject> files = List.of(new FileStorageObject());

    when(duosUser.getUser()).thenReturn(user);
    when(datasetService.findDatasetByIdForRead(user, datasetId)).thenReturn(new Dataset());
    when(fileStorageObjectService.fetchAllMetadataByEntityId(datasetId.toString()))
        .thenReturn(files);

    try (var response = resource.findDocumentsByEntity(duosUser, "DaTaSeT", datasetId.toString())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(files, response.getEntity());
    }
  }

  @Test
  void testFindDocumentsByStudyEntityReturnsMetadata() {
    Integer studyId = 456;
    Study study = new Study();
    study.setUuid(UUID.randomUUID());
    study.setPublicVisibility(true);
    List<FileStorageObject> files = List.of(new FileStorageObject());

    when(duosUser.getUser()).thenReturn(user);
    when(datasetService.findStudy(studyId)).thenReturn(study);
    when(fileStorageObjectService.fetchAllMetadataByEntityId(study.getUuid().toString()))
        .thenReturn(files);

    try (var response = resource.findDocumentsByEntity(duosUser, "study", studyId.toString())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(files, response.getEntity());
    }

    verify(fileStorageObjectService).fetchAllMetadataByEntityId(study.getUuid().toString());
  }

  @Test
  void testFindDocumentsByDatasetEntityNotFound() {
    Integer datasetId = 111;

    when(duosUser.getUser()).thenReturn(user);
    when(datasetService.findDatasetByIdForRead(user, datasetId))
        .thenThrow(new jakarta.ws.rs.NotFoundException("Entity not found"));

    try (var response = resource.findDocumentsByEntity(duosUser, "dataset", datasetId.toString())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testFindDocumentsByDatasetEntityForbidden() {
    Integer datasetId = 222;

    when(duosUser.getUser()).thenReturn(user);
    when(datasetService.findDatasetByIdForRead(user, datasetId))
        .thenThrow(new jakarta.ws.rs.ForbiddenException("User does not have permission"));

    try (var response = resource.findDocumentsByEntity(duosUser, "dataset", datasetId.toString())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testFindDocumentsByStudyEntityReturnsEmptyList() {
    Integer studyId = 333;
    Study study = new Study();
    study.setUuid(UUID.randomUUID());
    study.setPublicVisibility(true);

    when(duosUser.getUser()).thenReturn(user);
    when(datasetService.findStudy(studyId)).thenReturn(study);
    when(fileStorageObjectService.fetchAllMetadataByEntityId(study.getUuid().toString()))
        .thenReturn(List.of());

    try (var response = resource.findDocumentsByEntity(duosUser, "study", studyId.toString())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(List.of(), response.getEntity());
    }
  }

  @Test
  void testFindDocumentsByEntityUnauthenticatedForbidden() {
    try (var response = resource.findDocumentsByEntity(null, "dataset", "123")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testFindDocumentsByStudyEntityAuthorizationErrorReturnsForbidden() {
    Integer studyId = 444;
    Study study = new Study();
    study.setUuid(UUID.randomUUID());
    study.setPublicVisibility(false);

    when(duosUser.getUser()).thenReturn(user);
    when(datasetService.findStudy(studyId)).thenReturn(study);
    doThrow(new NullPointerException("auth failure"))
        .when(datasetService)
        .isCreatorCustodianOrAdmin(user, study);

    try (var response = resource.findDocumentsByEntity(duosUser, "study", studyId.toString())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }
}
