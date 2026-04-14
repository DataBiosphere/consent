package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.DocumentEntity;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.FileStorageObjectService;

@Path("api/{entity}")
public class DocumentResource extends Resource {

  private final DatasetService datasetService;
  private final FileStorageObjectService fileStorageObjectService;

  @Inject
  public DocumentResource(
      DatasetService datasetService, FileStorageObjectService fileStorageObjectService) {
    this.datasetService = datasetService;
    this.fileStorageObjectService = fileStorageObjectService;
  }

  @GET
  @Path("{entityId}/document")
  @Produces(MediaType.APPLICATION_JSON)
  @PermitAll
  public Response findDocumentsByEntity(
      @Auth DuosUser duosUser,
      @PathParam("entity") String entity,
      @PathParam("entityId") String entityId) {
    try {
      User user = duosUser.getUser();
      String fsoEntityId = validateEntityAndReadAccess(entity, entityId, user);
      List<FileStorageObject> fileStorageObjects =
          fileStorageObjectService.fetchAllMetadataByEntityId(fsoEntityId);
      return Response.ok(fileStorageObjects).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  private String validateEntityAndReadAccess(String entity, String entityId, User user) {
    DocumentEntity documentEntity =
        DocumentEntity.fromValue(entity)
            .orElseThrow(() -> new NotFoundException("Entity not found"));

    return switch (documentEntity) {
      case DATASET -> validateDatasetReadAccess(entityId, user);
      case STUDY -> validateStudyReadAccess(entityId, user);
    };
  }

  private String validateDatasetReadAccess(String entityId, User user) {
    Integer datasetId = parseNumericEntityId(entityId);
    datasetService.findDatasetByIdForRead(user, datasetId);
    return datasetId.toString();
  }

  private String validateStudyReadAccess(String entityId, User user) {
    Integer studyId = parseNumericEntityId(entityId);
    Study study = datasetService.findStudy(studyId);
    if (study == null || study.getUuid() == null) {
      throw new NotFoundException("Entity not found");
    }
    if (!Boolean.TRUE.equals(study.getPublicVisibility())
        && !datasetService.isCreatorCustodianOrAdmin(user, study)) {
      throw new ForbiddenException("User does not have permission");
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
}
