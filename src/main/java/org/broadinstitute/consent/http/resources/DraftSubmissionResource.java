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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DraftSubmission;
import org.broadinstitute.consent.http.models.DraftSubmissionInterface;
import org.broadinstitute.consent.http.models.DraftSubmissionSummary;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.DraftSubmissionService;
import org.broadinstitute.consent.http.service.UserService;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

@Path("api/draft")
public class DraftSubmissionResource extends Resource {

  private final UserService userService;
  private final DraftSubmissionService draftSubmissionService;

  @Inject
  public DraftSubmissionResource(UserService userService,
      DraftSubmissionService draftSubmissionService) {
    this.userService = userService;
    this.draftSubmissionService = draftSubmissionService;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Path("/v1")
  @RolesAllowed({ADMIN, DATASUBMITTER})
  public Response getDraftSubmissions(@Auth AuthUser authUser) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      Set<DraftSubmissionSummary> draftSummariesSet = draftSubmissionService.findDraftSummeriesForUser(
          user);
      return Response.ok().entity(draftSummariesSet).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }

  }

  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Path("/v1")
  @RolesAllowed({ADMIN, DATASUBMITTER})
  public Response createDraftRegistration(@Auth AuthUser authUser, String json) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      DraftSubmissionInterface draft = new DraftSubmission(json, user);
      draftSubmissionService.insertDraftSubmission(draft);
      URI uri = UriBuilder.fromPath(String.format("/api/draft/v1/%s", draft.getUUID().toString()))
          .build();
      return Response.created(uri).entity(json).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Path("/v1/{draftUUID}")
  @RolesAllowed({ADMIN, DATASUBMITTER})
  public Response getDraftDocument(@Auth AuthUser authUser,
      @PathParam("draftUUID") String draftUUID) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      DraftSubmissionInterface draft = draftSubmissionService.getAuthorizedDraft(
          validateUUID(draftUUID), user);
      StreamingOutput output = draftSubmissionService.draftAsJson(draft);
      return Response.ok().entity(output).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }

  }

  @PUT
  @Produces({MediaType.APPLICATION_JSON})
  @Consumes({MediaType.APPLICATION_JSON})
  @Path("/v1/{draftUUID}")
  @RolesAllowed({ADMIN, DATASUBMITTER})
  public Response updateDraft(@Auth AuthUser authUser, @PathParam("draftUUID") String draftUUID,
      String json) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      DraftSubmissionInterface draft = draftSubmissionService.getAuthorizedDraft(
          validateUUID(draftUUID), user);
      draft.setJson(json);
      draftSubmissionService.updateDraftSubmission(draft, user);
      return Response.ok().entity(draftSubmissionService.draftAsJson(draft)).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/v1/{draftUUID}")
  @RolesAllowed({ADMIN, DATASUBMITTER})
  public Response deleteDraft(@Auth AuthUser authUser, @PathParam("draftUUID") String draftUUID) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      DraftSubmissionInterface draft = draftSubmissionService.getAuthorizedDraft(
          validateUUID(draftUUID), user);
      draftSubmissionService.deleteDraftSubmission(draft, user);
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
    return Response.ok().build();
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Path("/v1/{draftUUID}/attachments")
  @RolesAllowed({ADMIN, DATASUBMITTER})
  public Response getAttachments(@Auth AuthUser authUser,
      @PathParam("draftUUID") String draftUUID) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      DraftSubmissionInterface draft = draftSubmissionService.getAuthorizedDraft(
          validateUUID(draftUUID), user);
      return Response.ok().entity(draft.getStoredFiles()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }

  }

  @POST
  @Produces({MediaType.APPLICATION_JSON})
  @Consumes({MediaType.MULTIPART_FORM_DATA})
  @Path("/v1/{draftUUID}/attachments")
  @RolesAllowed({ADMIN, DATASUBMITTER})
  public Response addAttachments(@Auth AuthUser authUser, @PathParam("draftUUID") String draftUUID,
      FormDataMultiPart multipart) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      DraftSubmissionInterface draft = draftSubmissionService.getAuthorizedDraft(
          validateUUID(draftUUID), user);
      Map<String, FormDataBodyPart> files = extractFilesFromMultiPart(multipart);
      DraftSubmissionInterface updatedDraft = draftSubmissionService.addAttachments(draft, user,
          files);
      return Response.ok().entity(updatedDraft.getStoredFiles()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("/v1/{draftUUID}/attachments/{fileId}")
  @RolesAllowed({ADMIN, DATASUBMITTER})
  public Response getAttachment(@Auth AuthUser authUser, @PathParam("draftUUID") String draftUUID,
      @PathParam("fileId") Integer fileId) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      DraftSubmissionInterface draft = draftSubmissionService.getAuthorizedDraft(
          validateUUID(draftUUID), user);
      Set<FileStorageObject> filteredAttachments = draft.getStoredFiles().stream()
          .filter((fileStorageObject) -> fileStorageObject.getFileStorageObjectId().equals(fileId))
          .collect(
              Collectors.toSet());
      if (filteredAttachments.isEmpty()) {
        return Response.status(Response.Status.NOT_FOUND).build();
      } else if (filteredAttachments.size() == 1) {
        FileStorageObject targetAttachment = filteredAttachments.iterator().next();
        InputStream fileStream = draftSubmissionService.getDraftAttachmentStream(targetAttachment);
        StreamingOutput streamOutput = createStreamingOutput(fileStream);
        return Response.ok(streamOutput).header(HttpHeaders.CONTENT_DISPOSITION,
            String.format("attachment; filename=\"%s\"", targetAttachment.getFileName())).build();
      } else {
        logWarn(String.format(
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
  @RolesAllowed({ADMIN, DATASUBMITTER})
  public Response deleteDraftAttachment(@Auth AuthUser authUser,
      @PathParam("draftUUID") String draftUUID, @PathParam("fileId") Integer fileId) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      DraftSubmissionInterface draft = draftSubmissionService.getAuthorizedDraft(
          validateUUID(draftUUID), user);
      draftSubmissionService.deleteDraftAttachment(draft, user, fileId);
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  private URI getDraftURI(DraftSubmissionInterface draft) {
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