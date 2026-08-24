package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriBuilder;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.models.DraftInterface;
import org.broadinstitute.consent.http.models.DraftStudyDataset;
import org.broadinstitute.consent.http.models.DraftSummary;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.DraftService;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

@Path("api/draft")
public class DraftResource extends Resource {

  private final DraftService draftService;

  @Inject
  public DraftResource(DraftService draftService) {
    this.draftService = draftService;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Path("/v1")
  @RolesAllowed({ADMIN, CHAIRPERSON, DATASUBMITTER})
  public Response getDrafts(@Auth DuosUser duosUser) {
    try {
      User user = duosUser.getUser();
      Collection<DraftSummary> draftSummaries = draftService.findDraftSummariesForUser(user);
      return Response.ok().entity(draftSummaries).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Path("/v1")
  @RolesAllowed({ADMIN, CHAIRPERSON, DATASUBMITTER})
  public Response createDraftRegistration(@Auth DuosUser duosUser, String json) {
    try {
      User user = duosUser.getUser();
      DraftInterface draft = new DraftStudyDataset(json, user);
      draftService.insertDraft(draft);
      URI uri = getDraftURI(draft);
      // The stored document, not the posted one: the write drops what Postgres cannot hold.
      return Response.created(uri).entity(draft.getJson()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Path("/v1/{draftUUID}")
  @RolesAllowed({ADMIN, CHAIRPERSON, DATASUBMITTER})
  public Response getDraftDocument(
      @Auth DuosUser duosUser, @PathParam("draftUUID") String draftUUID) {
    try {
      User user = duosUser.getUser();
      DraftInterface draft = draftService.getAuthorizedDraft(validateUUID(draftUUID), user);
      StreamingOutput output = draftService.draftAsJson(draft);
      return Response.ok().entity(output).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @Produces({MediaType.APPLICATION_JSON})
  @Consumes({MediaType.APPLICATION_JSON})
  @Path("/v1/{draftUUID}")
  @RolesAllowed({ADMIN, CHAIRPERSON, DATASUBMITTER})
  public Response updateDraft(
      @Auth DuosUser duosUser, @PathParam("draftUUID") String draftUUID, String json) {
    try {
      User user = duosUser.getUser();
      DraftInterface draft = draftService.getAuthorizedDraft(validateUUID(draftUUID), user);
      draft.setJson(json);
      DraftInterface responseDraft = draftService.updateDraft(draft, user);
      return Response.ok().entity(draftService.draftAsJson(responseDraft)).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PATCH
  @Produces({MediaType.APPLICATION_JSON})
  @Consumes({MediaType.TEXT_PLAIN})
  @Path("/v1/{draftUUID}")
  @RolesAllowed({ADMIN, CHAIRPERSON, DATASUBMITTER})
  public Response patchDraftName(
      @Auth DuosUser duosUser, @PathParam("draftUUID") String draftUUID, String name) {
    try {
      User user = duosUser.getUser();
      DraftInterface draft = draftService.getAuthorizedDraft(validateUUID(draftUUID), user);
      draft.setName(name);
      DraftInterface responseDraft = draftService.updateDraft(draft, user);
      return Response.ok().entity(draftService.draftAsJson(responseDraft)).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/v1/{draftUUID}")
  @RolesAllowed({ADMIN, CHAIRPERSON, DATASUBMITTER})
  public Response deleteDraft(@Auth DuosUser duosUser, @PathParam("draftUUID") String draftUUID) {
    try {
      User user = duosUser.getUser();
      DraftInterface draft = draftService.getAuthorizedDraft(validateUUID(draftUUID), user);
      draftService.deleteDraft(draft, user);
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
    return Response.ok().build();
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Path("/v1/{draftUUID}/attachments")
  @RolesAllowed({ADMIN, CHAIRPERSON, DATASUBMITTER})
  public Response getAttachments(
      @Auth DuosUser duosUser, @PathParam("draftUUID") String draftUUID) {
    try {
      User user = duosUser.getUser();
      DraftInterface draft = draftService.getAuthorizedDraft(validateUUID(draftUUID), user);
      return Response.ok().entity(draft.getStoredFiles()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Produces({MediaType.APPLICATION_JSON})
  @Consumes({MediaType.MULTIPART_FORM_DATA})
  @Path("/v1/{draftUUID}/attachments")
  @RolesAllowed({ADMIN, CHAIRPERSON, DATASUBMITTER})
  public Response addAttachments(
      @Auth DuosUser duosUser,
      @PathParam("draftUUID") String draftUUID,
      FormDataMultiPart multipart) {
    try {
      User user = duosUser.getUser();
      DraftInterface draft = draftService.getAuthorizedDraft(validateUUID(draftUUID), user);
      Map<String, FormDataBodyPart> files = extractFilesFromMultiPart(multipart);
      List<FileStorageObject> storedFiles = draftService.addAttachments(draft, user, files);
      return Response.ok().entity(storedFiles).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("/v1/{draftUUID}/attachments/{fileId}")
  @RolesAllowed({ADMIN, CHAIRPERSON, DATASUBMITTER})
  public Response getAttachment(
      @Auth DuosUser duosUser,
      @PathParam("draftUUID") String draftUUID,
      @PathParam("fileId") Integer fileId) {
    try {
      User user = duosUser.getUser();
      DraftInterface draft = draftService.getAuthorizedDraft(validateUUID(draftUUID), user);
      Set<FileStorageObject> filteredAttachments =
          draft.getStoredFiles().stream()
              .filter(
                  (fileStorageObject) -> fileStorageObject.getFileStorageObjectId().equals(fileId))
              .collect(Collectors.toSet());
      if (filteredAttachments.isEmpty()) {
        return Response.status(Response.Status.NOT_FOUND).build();
      } else if (filteredAttachments.size() == 1) {
        FileStorageObject targetAttachment = filteredAttachments.iterator().next();
        InputStream fileStream = draftService.getDraftAttachmentStream(targetAttachment);
        StreamingOutput streamOutput = createStreamingOutput(fileStream);
        return Response.ok(streamOutput)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                String.format("attachment; filename=\"%s\"", targetAttachment.getFileName()))
            .build();
      } else {
        logWarn(
            String.format(
                "More than one file attachment matches requested draft ID and file ID combination.  draftid: %s, fileid: %d",
                draftUUID, fileId));
        throw new InternalServerErrorException("Disambiguation error encountered.");
      }
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/v1/{draftUUID}/attachments/{fileId}")
  @RolesAllowed({ADMIN, CHAIRPERSON, DATASUBMITTER})
  public Response deleteDraftAttachment(
      @Auth DuosUser duosUser,
      @PathParam("draftUUID") String draftUUID,
      @PathParam("fileId") Integer fileId) {
    try {
      User user = duosUser.getUser();
      DraftInterface draft = draftService.getAuthorizedDraft(validateUUID(draftUUID), user);
      draftService.deleteDraftAttachment(draft, user, fileId);
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  private URI getDraftURI(DraftInterface draft) {
    if (draft.getUUID() != null) {
      return UriBuilder.fromPath(String.format("/api/draft/v1/%s", draft.getUUID().toString()))
          .build();
    }
    throw new NotFoundException("Draft not found.");
  }

  private UUID validateUUID(String uuid) {
    try {
      return UUID.fromString(uuid.trim());
    } catch (Exception e) {
      throw new BadRequestException(String.format("Invalid UUID: %s", uuid));
    }
  }
}
