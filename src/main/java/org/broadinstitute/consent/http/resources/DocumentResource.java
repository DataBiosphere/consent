package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.FileStorageObjectService;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Path("api/{entity}")
public class DocumentResource extends Resource {

  private final FileStorageObjectService fileStorageObjectService;

  @Inject
  public DocumentResource(FileStorageObjectService fileStorageObjectService) {
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
      List<FileStorageObject> fileStorageObjects =
          fileStorageObjectService.fetchAllMetadataByEntityAndEntityIdForRead(
              user, entity, entityId);
      return Response.ok(fileStorageObjects).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
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

  @POST
  @Path("{entityId}/document")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @PermitAll
  public Response uploadDocument(
      @Auth DuosUser duosUser,
      @PathParam("entity") String entity,
      @PathParam("entityId") String entityId,
      @FormDataParam("file") InputStream file,
      @FormDataParam("file") FormDataContentDisposition fileDetail,
      @FormDataParam("category") String categoryStr) {
    try {
      validateFileDetails(fileDetail);
      User user = duosUser.getUser();
      FileStorageObject created =
          fileStorageObjectService.uploadDocument(
              user, entity, entityId, file, fileDetail, categoryStr);
      return Response.status(Response.Status.CREATED).entity(created).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
