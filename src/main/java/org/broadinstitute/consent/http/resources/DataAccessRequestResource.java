package org.broadinstitute.consent.http.resources;

import com.google.cloud.storage.BlobId;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.enumeration.DarDocumentType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Error;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.EmailService;
import org.broadinstitute.consent.http.service.MatchService;
import org.broadinstitute.consent.http.service.UserService;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Path("api/dar")
public class DataAccessRequestResource extends Resource {

  private final DataAccessRequestService dataAccessRequestService;
  private final EmailService emailService;
  private final GCSService gcsService;
  private final MatchService matchService;
  private final UserService userService;
  private final DatasetService datasetService;

  @Inject
  public DataAccessRequestResource(
      DataAccessRequestService dataAccessRequestService,
      EmailService emailService,
      GCSService gcsService,
      UserService userService,
      DatasetService datasetService,
      MatchService matchService
  ) {
    this.dataAccessRequestService = dataAccessRequestService;
    this.emailService = emailService;
    this.gcsService = gcsService;
    this.userService = userService;
    this.datasetService = datasetService;
    this.matchService = matchService;
  }

  @GET
  @Produces("application/json")
  @PermitAll
  @Path("/v2")
  public Response getDataAccessRequests(@Auth AuthUser authUser) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      List<DataAccessRequest> dars = dataAccessRequestService.getDataAccessRequestsByUserRole(user);
      return Response.ok().entity(dars).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Consumes("application/json")
  @Produces("application/json")
  @RolesAllowed(RESEARCHER)
  @Path("/v2")
  public Response createDataAccessRequest(
      @Auth AuthUser authUser, @Context UriInfo info, String dar) {
    try {
      User user = findUserByEmail(authUser.getEmail());
      if (Objects.isNull(user.getLibraryCards()) || user.getLibraryCards().isEmpty()) {
        throw new IllegalArgumentException("User must have a library card to create a DAR.");
      }

      DataAccessRequest payload = populateDarFromJsonString(user, dar);
      DataAccessRequest newDar = dataAccessRequestService.createDataAccessRequest(user, payload);
      Integer collectionId = newDar.getCollectionId();
      try {
        emailService.sendNewDARCollectionMessage(collectionId);
      } catch (Exception e) {
        // non-fatal exception
        logException("Exception sending email for collection id: " + collectionId, e);
      }
      URI uri = info.getRequestUriBuilder().build();
      matchService.reprocessMatchesForPurpose(newDar.getReferenceId());
      return Response.created(uri).entity(newDar.convertToSimplifiedDar()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Consumes("application/json")
  @Produces("application/json")
  @RolesAllowed(RESEARCHER)
  @Path("/v3")
  public Response createDataAccessRequestWithDAARestrictions(
      @Auth AuthUser authUser, @Context UriInfo info, String dar) {
    try {
      // TODO: Do the DAA Enforcement
      User user = findUserByEmail(authUser.getEmail());
      if (Objects.isNull(user.getLibraryCards()) || user.getLibraryCards().isEmpty()) {
        throw new IllegalArgumentException("User must have a library card to create a DAR.");
      }

      DataAccessRequest payload = populateDarFromJsonString(user, dar);
      DataAccessRequest newDar = dataAccessRequestService.createDataAccessRequest(user, payload);
      Integer collectionId = newDar.getCollectionId();
      try {
        emailService.sendNewDARCollectionMessage(collectionId);
      } catch (Exception e) {
        // non-fatal exception
        logException("Exception sending email for collection id: " + collectionId, e);
      }
      URI uri = info.getRequestUriBuilder().build();
      matchService.reprocessMatchesForPurpose(newDar.getReferenceId());
      return Response.created(uri).entity(newDar.convertToSimplifiedDar()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("/v2/{referenceId}")
  @Produces("application/json")
  @PermitAll
  public Response getByReferenceId(
      @Auth AuthUser authUser, @PathParam("referenceId") String referenceId) {
    validateAuthedRoleUser(
        Stream.of(UserRoles.ADMIN, UserRoles.CHAIRPERSON, UserRoles.MEMBER)
            .collect(Collectors.toList()),
        authUser, referenceId);
    try {
      DataAccessRequest dar = dataAccessRequestService.findByReferenceId(referenceId);
      if (Objects.nonNull(dar)) {
        return Response.status(Response.Status.OK).entity(dar.convertToSimplifiedDar()).build();
      }
      return Response.status(Response.Status.NOT_FOUND)
          .entity(
              new Error(
                  "Unable to find Data Access Request with reference id: " + referenceId,
                  Response.Status.NOT_FOUND.getStatusCode()))
          .build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @Path("/v2/{referenceId}")
  @Produces("application/json")
  @RolesAllowed(RESEARCHER)
  public Response updateByReferenceId(
      @Auth AuthUser authUser, @PathParam("referenceId") String referenceId, String dar) {
    try {
      User user = findUserByEmail(authUser.getEmail());
      DataAccessRequest originalDar = dataAccessRequestService.findByReferenceId(referenceId);
      checkAuthorizedUpdateUser(user, originalDar);
      DataAccessRequestData data = DataAccessRequestData.fromString(dar);
      // Keep dar data reference id in sync with the dar until we fully deprecate
      // it in dar data.
      data.setReferenceId(originalDar.getReferenceId());
      originalDar.setData(data);
      DataAccessRequest updatedDar =
          dataAccessRequestService.updateByReferenceId(user, originalDar);
      matchService.reprocessMatchesForPurpose(referenceId);
      return Response.ok().entity(updatedDar.convertToSimplifiedDar()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces("application/json")
  @Path("/v2/draft")
  @RolesAllowed(RESEARCHER)
  public Response getDraftDataAccessRequests(@Auth AuthUser authUser) {
    try {
      User user = findUserByEmail(authUser.getEmail());
      List<DataAccessRequest> draftDars = dataAccessRequestService.findAllDraftDataAccessRequestsByUser(
          user.getUserId());
      return Response.ok().entity(draftDars).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces("application/json")
  @Path("/v2/draft/{referenceId}")
  @RolesAllowed(RESEARCHER)
  public Response getDraftDar(@Auth AuthUser authUser, @PathParam("referenceId") String id) {
    try {
      User user = findUserByEmail(authUser.getEmail());
      DataAccessRequest dar = dataAccessRequestService.findByReferenceId(id);
      if (dar.getUserId().equals(user.getUserId())) {
        return Response.ok().entity(dar).build();
      }
      throw new ForbiddenException("User does not have permission");
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Consumes("application/json")
  @Produces("application/json")
  @Path("/v2/draft")
  @RolesAllowed(RESEARCHER)
  public Response createDraftDataAccessRequest(
      @Auth AuthUser authUser, @Context UriInfo info, String dar) {
    try {
      User user = findUserByEmail(authUser.getEmail());
      DataAccessRequest newDar = populateDarFromJsonString(user, dar);
      DataAccessRequest result =
          dataAccessRequestService.insertDraftDataAccessRequest(user, newDar);
      URI uri = info.getRequestUriBuilder().path("/" + result.getReferenceId()).build();
      return Response.created(uri).entity(result.convertToSimplifiedDar()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Consumes("application/json")
  @Produces("application/json")
  @Path("/v3/draft")
  @RolesAllowed(RESEARCHER)
  public Response createDraftDataAccessRequestWithDAARestrictions(
      @Auth AuthUser authUser, @Context UriInfo info, String dar) {
    try {
      // TODO: Do the DAA Enforcement
      User user = findUserByEmail(authUser.getEmail());
      DataAccessRequest newDar = populateDarFromJsonString(user, dar);
      DataAccessRequest result =
          dataAccessRequestService.insertDraftDataAccessRequest(user, newDar);
      URI uri = info.getRequestUriBuilder().path("/" + result.getReferenceId()).build();
      return Response.created(uri).entity(result.convertToSimplifiedDar()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @Consumes("application/json")
  @Produces("application/json")
  @Path("/v2/draft/{referenceId}")
  @RolesAllowed(RESEARCHER)
  public Response updatePartialDataAccessRequest(
      @Auth AuthUser authUser, @PathParam("referenceId") String referenceId, String dar) {
    try {
      User user = findUserByEmail(authUser.getEmail());
      DataAccessRequest originalDar = dataAccessRequestService.findByReferenceId(referenceId);
      checkAuthorizedUpdateUser(user, originalDar);
      DataAccessRequestData data = DataAccessRequestData.fromString(dar);
      // Keep dar data reference id in sync with the dar until we fully deprecate
      // it in dar data.
      data.setReferenceId(originalDar.getReferenceId());
      originalDar.setData(data);
      originalDar.setDatasetIds(data.getDatasetIds());
      DataAccessRequest updatedDar =
          dataAccessRequestService.updateByReferenceId(user, originalDar);
      return Response.ok().entity(updatedDar.convertToSimplifiedDar()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @Consumes("application/json")
  @Produces("application/json")
  @Path("/v3/draft/{referenceId}")
  @RolesAllowed(RESEARCHER)
  public Response updatePartialDataAccessRequestWithDAARestrictions(
      @Auth AuthUser authUser, @PathParam("referenceId") String referenceId, String dar) {
    try {
      // TODO: Do the DAA Enforcement
      User user = findUserByEmail(authUser.getEmail());
      DataAccessRequest originalDar = dataAccessRequestService.findByReferenceId(referenceId);
      checkAuthorizedUpdateUser(user, originalDar);
      DataAccessRequestData data = DataAccessRequestData.fromString(dar);
      // Keep dar data reference id in sync with the dar until we fully deprecate
      // it in dar data.
      data.setReferenceId(originalDar.getReferenceId());
      originalDar.setData(data);
      originalDar.setDatasetIds(data.getDatasetIds());
      DataAccessRequest updatedDar =
          dataAccessRequestService.updateByReferenceId(user, originalDar);
      return Response.ok().entity(updatedDar.convertToSimplifiedDar()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
  @Path("/v2/{referenceId}/irbDocument")
  @RolesAllowed({ADMIN, CHAIRPERSON, MEMBER, RESEARCHER})
  public Response getIrbDocument(
      @Auth AuthUser authUser,
      @PathParam("referenceId") String referenceId) {
    try {
      DataAccessRequest dar = getDarById(referenceId);
      validateAuthedRoleUser(
          Stream.of(UserRoles.ADMIN, UserRoles.CHAIRPERSON, UserRoles.MEMBER)
              .collect(Collectors.toList()),
          authUser, referenceId);
      if (dar.getData() != null &&
          StringUtils.isNotEmpty(dar.getData().getIrbDocumentLocation()) &&
          StringUtils.isNotEmpty(dar.getData().getIrbDocumentName())
      ) {
        String blobIdName = dar.getData().getIrbDocumentLocation();
        String fileName = dar.getData().getIrbDocumentName();
        InputStream is = gcsService.getDocument(blobIdName);
        StreamingOutput stream = createStreamingOutput(is);
        return Response.ok(stream)
            .header("Content-Disposition", "attachment; filename=" + fileName)
            .build();
      }
      throw new NotFoundException();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/v2/{referenceId}/irbDocument")
  @RolesAllowed({RESEARCHER})
  public Response uploadIrbDocument(
      @Auth AuthUser authUser,
      @PathParam("referenceId") String referenceId,
      @FormDataParam("file") InputStream uploadInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    try {
      User user = findUserByEmail(authUser.getEmail());
      DataAccessRequest dar = getDarById(referenceId);
      checkAuthorizedUpdateUser(user, dar);
      DataAccessRequest updatedDar = updateDarWithDocumentContents(DarDocumentType.IRB, user, dar,
          uploadInputStream, fileDetail);
      return Response.ok(updatedDar.convertToSimplifiedDar()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/v2/progress_report/{parentReferenceId}")
  @RolesAllowed({RESEARCHER})
  public Response postProgressReport(
      @Auth AuthUser authUser,
      @PathParam("parentReferenceId") String parentReferenceId,
      @FormDataParam("dar") String dar,
      @FormDataParam("collaboratorRequiredFile") InputStream collabInputStream,
      @FormDataParam("collaboratorRequiredFile") FormDataContentDisposition collabFileDetails,
      @FormDataParam("ethicsApprovalRequiredFile") InputStream ethicsInputStream,
      @FormDataParam("ethicsApprovalRequiredFile") FormDataContentDisposition ethicsFileDetails) {
    User user = userService.findUserByEmail(authUser.getEmail());
    DataAccessRequest parentDar = dataAccessRequestService.findByReferenceId(parentReferenceId);
    checkAuthorizedUpdateUser(user, parentDar);
    DataAccessRequest payload = populateDarFromJsonString(user, dar);
    DataAccessRequest childDar = dataAccessRequestService.createDataAccessRequest(user, payload);

    for (Integer datasetId : childDar.getDatasetIds()) {
      Dataset dataset = datasetService.findDatasetById(datasetId);
      if (dataset == null) {
        throw new NotFoundException("Dataset " + datasetId + " not found");
      }
      DataUse dataUse = dataset.getDataUse();
      if (dataUse == null || dataUse.getCollaboratorRequired() == null
          || dataUse.getEthicsApprovalRequired() == null) {
        throw new BadRequestException("Dataset " + datasetId + " is missing data use(s)");
      }
      if (dataUse.getCollaboratorRequired()) {
        String parentCollabLocation = parentDar.getData().getCollaborationLetterLocation();
        if ((collabFileDetails == null || collabFileDetails.getSize() <= 0)
            && Strings.isNullOrEmpty(parentCollabLocation)) {
          throw new BadRequestException("Collaboration document is required");
        }
        try {
          childDar = updateDarWithDocumentContents(DarDocumentType.COLLABORATION, user, childDar,
              collabInputStream, collabFileDetails);
        } catch (IOException e) {
          return createExceptionResponse(e);
        }
      }
      if (dataUse.getEthicsApprovalRequired()) {
        String parentEthicsLocation = parentDar.getData().getIrbDocumentLocation();
        if ((ethicsFileDetails == null || ethicsFileDetails.getSize() <= 0)
            && Strings.isNullOrEmpty(parentEthicsLocation)) {
          throw new BadRequestException("Ethics approval document is required");
        }
        try {
          childDar = updateDarWithDocumentContents(DarDocumentType.IRB, user, childDar,
              ethicsInputStream, ethicsFileDetails);
        } catch (IOException e) {
          return createExceptionResponse(e);
        }
      }
    }

    return Response.ok(childDar.convertToSimplifiedDar()).build();
  }

  @GET
  @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
  @Path("/v2/{referenceId}/collaborationDocument")
  @RolesAllowed({ADMIN, CHAIRPERSON, MEMBER, RESEARCHER})
  public Response getCollaborationDocument(
      @Auth AuthUser authUser,
      @PathParam("referenceId") String referenceId) {
    try {
      DataAccessRequest dar = getDarById(referenceId);
      validateAuthedRoleUser(
          Stream.of(UserRoles.ADMIN, UserRoles.CHAIRPERSON, UserRoles.MEMBER)
              .collect(Collectors.toList()),
          authUser, referenceId);
      if (dar.getData() != null &&
          StringUtils.isNotEmpty(dar.getData().getCollaborationLetterLocation()) &&
          StringUtils.isNotEmpty(dar.getData().getCollaborationLetterName())
      ) {
        String blobIdName = dar.getData().getCollaborationLetterLocation();
        String fileName = dar.getData().getCollaborationLetterName();
        InputStream is = gcsService.getDocument(blobIdName);
        StreamingOutput stream = createStreamingOutput(is);
        return Response.ok(stream)
            .header("Content-Disposition", "attachment; filename=" + fileName)
            .build();
      }
      throw new NotFoundException();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/v2/{referenceId}/collaborationDocument")
  @RolesAllowed({RESEARCHER})
  public Response uploadCollaborationDocument(
      @Auth AuthUser authUser,
      @PathParam("referenceId") String referenceId,
      @FormDataParam("file") InputStream uploadInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    try {
      User user = findUserByEmail(authUser.getEmail());
      DataAccessRequest dar = getDarById(referenceId);
      checkAuthorizedUpdateUser(user, dar);
      DataAccessRequest updatedDar = updateDarWithDocumentContents(DarDocumentType.COLLABORATION,
          user, dar, uploadInputStream, fileDetail);
      return Response.ok(updatedDar.convertToSimplifiedDar()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @Path("/v2/{referenceId}")
  @Produces("application/json")
  @RolesAllowed({ADMIN, RESEARCHER})
  public Response deleteDar(@Auth AuthUser authUser, @PathParam("referenceId") String referenceId) {
    validateAuthedRoleUser(Collections.singletonList(UserRoles.ADMIN), authUser, referenceId);
    try {
      User user = findUserByEmail(authUser.getEmail());
      dataAccessRequestService.deleteByReferenceId(user, referenceId);
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  private User findUserByEmail(String email) {
    User user = userService.findUserByEmail(email);
    if (user == null) {
      throw new NotFoundException("Unable to find User with the provided email: " + email);
    }
    return user;
  }

  private DataAccessRequest populateDarFromJsonString(User user, String json) {
    DataAccessRequest newDar = new DataAccessRequest();
    DataAccessRequestData data;
    try {
      data = DataAccessRequestData.fromString(json);
    } catch (Exception e) {
      throw new BadRequestException("Unable to parse DAR from JSON string");
    }
    if (Objects.isNull(data)) {
      data = new DataAccessRequestData();
    }
    // When posting a submitted dar, there are two cases:
    // 1. those that existed previously as a draft dar
    // 2. those that are brand new
    // Validate the provided referenceId with the authenticated user and draft status
    // Those that do not validate are considered a brand new dar
    if (Objects.nonNull(data.getReferenceId())) {
      DataAccessRequest existingDar =
          dataAccessRequestService.findByReferenceId(data.getReferenceId());
      if (Objects.nonNull(existingDar)
          && existingDar.getUserId().equals(user.getUserId())
          && existingDar.getDraft()) {
        newDar.setReferenceId(data.getReferenceId());

        // if dar was part of a collection, we should use the same collection.
        if (Objects.nonNull(existingDar.getCollectionId())) {
          newDar.setCollectionId(existingDar.getCollectionId());
        }
      } else {
        String referenceId = UUID.randomUUID().toString();
        newDar.setReferenceId(referenceId);
        data.setReferenceId(referenceId);
      }
    } else {
      String referenceId = UUID.randomUUID().toString();
      newDar.setReferenceId(referenceId);
      data.setReferenceId(referenceId);
    }
    newDar.setData(data);
    newDar.addDatasetIds(data.getDatasetIds());
    return newDar;
  }

  private void checkAuthorizedUpdateUser(User user, DataAccessRequest dar) {
    if (!user.getUserId().equals(dar.getUserId())) {
      throw new ForbiddenException("User not authorized to update this Data Access Request");
    }
  }

  private DataAccessRequest updateDarWithDocumentContents(
      DarDocumentType type,
      User user,
      DataAccessRequest dar,
      InputStream uploadInputStream,
      FormDataContentDisposition fileDetail) throws IOException {
    validateFileDetails(fileDetail);
    String fileName = fileDetail.getFileName();
    UUID id = UUID.randomUUID();
    BlobId blobId = gcsService.storeDocument(uploadInputStream, fileDetail.getType(), id);
    switch (type) {
      case IRB:
        // Delete the current document if it exists
        if (Objects.nonNull(dar.getData().getIrbDocumentLocation())) {
          deleteDarDocument(dar, dar.getData().getIrbDocumentLocation());
        }
        dar.getData().setIrbDocumentLocation(blobId.getName());
        dar.getData().setIrbDocumentName(fileName);
        break;
      case COLLABORATION:
        // Delete the current document if it exists
        if (Objects.nonNull(dar.getData().getCollaborationLetterLocation())) {
          deleteDarDocument(dar, dar.getData().getCollaborationLetterLocation());
        }
        dar.getData().setCollaborationLetterLocation(blobId.getName());
        dar.getData().setCollaborationLetterName(fileName);
        break;
      default:
        break;
    }
    return dataAccessRequestService.updateByReferenceId(user, dar);
  }

  private void deleteDarDocument(DataAccessRequest dar, String blobIdName) {
    try {
      gcsService.deleteDocument(blobIdName);
    } catch (Exception e) {
      String message = String.format(
          "Unable to delete document for DAR ID: %s; dar document location: %s",
          dar.getReferenceId(), blobIdName);
      logWarn(message);
    }
  }

  private DataAccessRequest getDarById(String referenceId) {
    DataAccessRequest dar = dataAccessRequestService.findByReferenceId(referenceId);
    if (Objects.isNull(dar)) {
      throw new NotFoundException();
    }
    return dar;
  }

  /**
   * Custom handler for validating that a user can access a DAR. User will have access if ANY of
   * these conditions are met: If the DAR create user is the same as the Auth User, then the user
   * can access the resource. If the user has any of the roles in allowableRoles, then the user can
   * access the resource. In practice, pass in allowableRoles for users that are not the create user
   * (i.e. Admin) so they can also have access to the DAR.
   *
   * @param allowableRoles List of roles that would allow the user to access the resource
   * @param authUser       The AuthUser
   * @param referenceId    The referenceId of the resource.
   */
  private void validateAuthedRoleUser(final List<UserRoles> allowableRoles, AuthUser authUser,
      String referenceId) {
    DataAccessRequest dataAccessRequest = getDarById(referenceId);
    User user = findUserByEmail(authUser.getEmail());
    if (Objects.nonNull(dataAccessRequest.getUserId()) && dataAccessRequest.getUserId() > 0) {
      super.validateAuthedRoleUser(allowableRoles, user, dataAccessRequest.getUserId());
    } else {
      logWarn("DataAccessRequest '" + referenceId + "' has an invalid userId");
      super.validateAuthedRoleUser(allowableRoles, user, dataAccessRequest.getUserId());
    }
  }
}
