package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.FileStorageObjectService;

@Path("api/{entity}")
public class DocumentResource extends Resource {

  private final FileStorageObjectService fileStorageObjectService;

  @Inject
  public DocumentResource(FileStorageObjectService fileStorageObjectService) {
    this.fileStorageObjectService = fileStorageObjectService;
  }

  @GET
  @Path("/{entityId}/document/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @PermitAll
  public Response findDocumentByEntity(
      @Auth DuosUser duosUser,
      @PathParam("entity") String entity,
      @PathParam("entityId") String entityId,
      @PathParam("id") Integer id) {
    try {
      User user = duosUser.getUser();
      FileStorageObject fileStorageObject =
          fileStorageObjectService.fetchMetadataByEntityAndEntityIdForRead(
              user, entity, entityId, id);
      return Response.ok(fileStorageObject).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
