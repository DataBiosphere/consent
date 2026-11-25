package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
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
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.enumeration.DarDocumentDeletionReason;
import org.broadinstitute.consent.http.enumeration.DarDocumentType;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.exceptions.LibraryCardRequiredException;
import org.broadinstitute.consent.http.exceptions.SubmittedDARCannotBeEditedException;
import org.broadinstitute.consent.http.filters.UploadSizeLimited;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetDaaSnapshot;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Error;
import org.broadinstitute.consent.http.models.SubmissionApproval;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.DaaService;
import org.broadinstitute.consent.http.service.DacService;
import org.broadinstitute.consent.http.service.DarCollectionService;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.MatchService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.dao.DataAccessRequestServiceDAO.DocumentSave;
import org.broadinstitute.consent.http.util.ComplianceLogger;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.server.ContainerRequest;

@Path("api/dar")
public class DataAccessRequestResource extends Resource {

  private final DaaService daaService;
  private final DacService dacService;
  private final DataAccessRequestService dataAccessRequestService;
  private final DarCollectionService darCollectionService;
  private final GCSService gcsService;
  private final MatchService matchService;
  private final UserService userService;
  private final DatasetService datasetService;

  @Inject
  public DataAccessRequestResource(
      DaaService daaService,
      DacService dacService,
      DataAccessRequestService dataAccessRequestService,
      GCSService gcsService,
      UserService userService,
      DatasetService datasetService,
      MatchService matchService,
      DarCollectionService darCollectionService) {
    this.daaService = daaService;
    this.dacService = dacService;
    this.dataAccessRequestService = dataAccessRequestService;
    this.gcsService = gcsService;
    this.userService = userService;
    this.datasetService = datasetService;
    this.matchService = matchService;
    this.darCollectionService = darCollectionService;
  }

  @GET
  @Produces("application/json")
  @PermitAll
  @Path("/v2")
  public Response getDataAccessRequests(@Auth DuosUser duosUser) {
    try {
      User user = duosUser.getUser();
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
      @Auth DuosUser duosUser, @Context Request request, @Context UriInfo info, String dar) {
    try {
      User user = duosUser.getUser();

      DataAccessRequest payload = populateDarFromJsonString(user, dar);
      DataAccessRequest newDar =
          dataAccessRequestService.createDataAccessRequest(
              user, payload, (ContainerRequest) request);
      processNewDarCollection(newDar.getCollectionId());
      URI uri = info.getRequestUriBuilder().build();
      matchService.reprocessMatchesForPurpose(newDar.getReferenceId());
      List<Dataset> datasets = datasetService.findDatasetsByIds(user, newDar.getDatasetIds());
      ComplianceLogger.logDARSubmission(
          user, datasets, ((ContainerRequest) request), HttpStatusCodes.STATUS_CODE_CREATED);
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
      @Auth DuosUser duosUser, @PathParam("referenceId") String referenceId) {
    try {
      validateAuthedRoleUser(
          List.of(
              UserRoles.ADMIN, UserRoles.CHAIRPERSON, UserRoles.MEMBER, UserRoles.SIGNINGOFFICIAL),
          duosUser,
          referenceId);
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

  @GET
  @Path("/v2/{referenceId}/daas")
  @Produces("application/json")
  @PermitAll
  public Response getDAAsByReferenceId(
      @Auth DuosUser duosUser, @PathParam("referenceId") String referenceId) {
    try {
      validateAuthedRoleUser(
          List.of(UserRoles.ADMIN, UserRoles.CHAIRPERSON, UserRoles.MEMBER), duosUser, referenceId);
      List<DataAccessAgreement> dataAccessAgreements = daaService.findByDarReferenceId(referenceId);
      return Response.status(Response.Status.OK).entity(dataAccessAgreements).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("/v2/{referenceId}/dataset-daa-snapshots")
  @Produces("application/json")
  @PermitAll
  public Response getDatasetDaaSnapshotsByReferenceId(
      @Auth DuosUser duosUser, @PathParam("referenceId") String referenceId) {
    try {
      validateAuthedRoleUser(
          List.of(
              UserRoles.ADMIN, UserRoles.CHAIRPERSON, UserRoles.MEMBER, UserRoles.SIGNINGOFFICIAL),
          duosUser,
          referenceId);
      Map<Integer, DatasetDaaSnapshot> snapshots =
          dataAccessRequestService.findDatasetDaaSnapshotsByReferenceId(referenceId);
      return Response.status(Response.Status.OK).entity(snapshots).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @Path("/v2/{referenceId}")
  @Produces("application/json")
  @RolesAllowed(RESEARCHER)
  public Response updateByReferenceId(
      @Auth DuosUser duosUser, @PathParam("referenceId") String referenceId, String dar) {
    try {
      User user = duosUser.getUser();
      DataAccessRequest originalDar = dataAccessRequestService.findByReferenceId(referenceId);
      checkAuthorizedUpdateUser(user, originalDar);
      DataAccessRequestData data = DataAccessRequestData.fromString(dar);
      List<String> keepFromRow = keepServerManagedFields(data, originalDar);
      // Keep dar data reference id in sync with the dar until we fully deprecate
      // it in dar data.
      data.setReferenceId(originalDar.getReferenceId());
      originalDar.setData(data);
      DataAccessRequest updatedDar =
          dataAccessRequestService.updateByReferenceId(user, originalDar, keepFromRow);
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
  public Response getDraftDataAccessRequests(@Auth DuosUser duosUser) {
    try {
      User user = duosUser.getUser();
      List<DataAccessRequest> draftDars =
          dataAccessRequestService.findAllDraftDataAccessRequestsByUser(user.getUserId());
      return Response.ok().entity(draftDars).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces("application/json")
  @Path("/v2/draft/{referenceId}")
  @RolesAllowed(RESEARCHER)
  public Response getDraftDar(@Auth DuosUser duosUser, @PathParam("referenceId") String id) {
    try {
      User user = duosUser.getUser();
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
      @Auth DuosUser duosUser, @Context UriInfo info, String dar) {
    try {
      User user = duosUser.getUser();
      DataAccessRequest newDar = populateDarFromJsonString(user, dar);
      // A new draft has no uploads yet, so it has no documents.
      newDar.getData().keepStoredDocuments(null);
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
      @Auth DuosUser duosUser, @PathParam("referenceId") String referenceId, String dar) {
    try {
      User user = duosUser.getUser();
      DataAccessRequest originalDar = dataAccessRequestService.findByReferenceId(referenceId);
      checkAuthorizedUpdateUser(user, originalDar);
      DataAccessRequestData data = DataAccessRequestData.fromString(dar);
      List<String> keepFromRow = keepServerManagedFields(data, originalDar);
      // Keep dar data reference id in sync with the dar until we fully deprecate
      // it in dar data.
      data.setReferenceId(originalDar.getReferenceId());
      originalDar.setData(data);
      originalDar.setDatasetIds(data.getDatasetIds());
      DataAccessRequest updatedDar =
          dataAccessRequestService.updateByReferenceId(user, originalDar, keepFromRow);
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
      @Auth DuosUser duosUser, @PathParam("referenceId") String referenceId) {
    try {
      DataAccessRequest dar = getDarById(referenceId);
      validateAuthedRoleUser(
          List.of(UserRoles.ADMIN, UserRoles.CHAIRPERSON, UserRoles.MEMBER), duosUser, referenceId);
      if (dar.getData() != null
          && StringUtils.isNotEmpty(dar.getData().getIrbDocumentLocation())
          && StringUtils.isNotEmpty(dar.getData().getIrbDocumentName())) {
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
  @UploadSizeLimited
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/v2/{referenceId}/irbDocument")
  @RolesAllowed({RESEARCHER})
  public Response uploadIrbDocument(
      @Auth DuosUser duosUser,
      @PathParam("referenceId") String referenceId,
      @FormDataParam("file") InputStream uploadInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    try {
      User user = duosUser.getUser();
      DataAccessRequest dar = getDarById(referenceId);
      checkAuthorizedUpdateUser(user, dar);
      DataAccessRequest updatedDar =
          updateDarWithDocumentContents(
              DarDocumentType.IRB, user, dar, uploadInputStream, fileDetail);
      return Response.ok(updatedDar.convertToSimplifiedDar()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @RolesAllowed({SIGNINGOFFICIAL})
  @Path("/{referenceId}/approveCloseout")
  public Response approveCloseout(
      @Auth DuosUser duosUser,
      @Context Request request,
      @PathParam("referenceId") String referenceId,
      String body) {
    try {
      SubmissionApproval approved = readSubmissionApproval(body, referenceId);
      dataAccessRequestService.approveDataAccessRequestCloseout(
          duosUser.getUser(), referenceId, approved);
      DataAccessRequest dar = getDarById(referenceId);
      List<Dataset> datasets =
          datasetService.findDatasetsByIds(duosUser.getUser(), dar.getDatasetIds());
      ComplianceLogger.logCloseoutApprovalBySigningOfficial(
          duosUser.getUser(), datasets, (ContainerRequest) request, HttpStatusCodes.STATUS_CODE_OK);
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @UploadSizeLimited
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/v2/progress_report/{parentReferenceId}")
  @RolesAllowed({RESEARCHER})
  public Response postProgressReport(
      @Auth DuosUser duosUser,
      @Context Request request,
      @PathParam("parentReferenceId") String parentReferenceId,
      @FormDataParam("dar") String dar,
      @FormDataParam("collaboratorRequiredFile") InputStream collabInputStream,
      @FormDataParam("collaboratorRequiredFile") FormDataContentDisposition collabFileDetails,
      @FormDataParam("ethicsApprovalRequiredFile") InputStream ethicsInputStream,
      @FormDataParam("ethicsApprovalRequiredFile") FormDataContentDisposition ethicsFileDetails) {
    try {
      User user = duosUser.getUser();
      // added here because other dataAccessRequestServices calls are invoked that do not normally
      // require this sequence.  hasValidActiveERACredentials will also check for a LC so no
      // additional LC check needed.
      userService.validateActiveERACredentials(user);
      DataAccessRequest parentDar = dataAccessRequestService.findByReferenceId(parentReferenceId);
      // needs to happen before docs are uploaded
      if (!user.getUserId().equals(parentDar.getUserId())) {
        throw new ForbiddenException("User not authorized to update this Data Access Request");
      }
      DataAccessRequest payload =
          DataAccessRequest.populateProgressReportFromJsonString(dar, parentDar);
      // Prevent creation if there are open DataAccess elections for the parent DAR. A closeout
      // cancels the collection's open elections instead.
      if (!payload.getIsCloseoutProgressReport()) {
        List<Election> openElections =
            dataAccessRequestService.findOpenElectionsByReferenceId(parentDar.getReferenceId());
        boolean hasOpenDataAccessElections =
            openElections.stream()
                .anyMatch(
                    election ->
                        election
                            .getElectionType()
                            .equalsIgnoreCase(ElectionType.DATA_ACCESS.getValue()));
        if (hasOpenDataAccessElections) {
          throw new BadRequestException(
              "Cannot create a progress report for a DAR with an open election: "
                  + parentDar.getReferenceId());
        }
      }
      DataAccessRequest progressReport =
          createProgressReportWithDocuments(
              user,
              (ContainerRequest) request,
              payload,
              parentDar,
              new ProgressReportDocumentUploads(
                  collabInputStream, collabFileDetails, ethicsInputStream, ethicsFileDetails));
      if (Objects.nonNull(progressReport) && !progressReport.getIsCloseoutProgressReport()) {
        processNewDarCollection(parentDar.getCollectionId());
      }
      List<Dataset> datasets =
          datasetService.findDatasetsByIds(user, progressReport.getDatasetIds());
      ComplianceLogger.logDARSubmission(
          user, datasets, ((ContainerRequest) request), HttpStatusCodes.STATUS_CODE_CREATED);
      return Response.ok(progressReport.convertToSimplifiedDar()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  private void processNewDarCollection(Integer collectionId) {
    try {
      // This service method knows how to distinguish between a DAR and a Progress Report.
      darCollectionService.createElectionsForNewDarCollection(collectionId);
      darCollectionService.sendNewDARCollectionMessage(collectionId);
    } catch (ConsentConflictException e) {
      // A closeout committed after this submission. Nothing may be opened on the collection.
      logWarn(
          "Skipping new DAR processing for collection id: %s: %s"
              .formatted(collectionId, e.getMessage()));
    } catch (Exception e) {
      // non-fatal exception
      logCaughtEmailException(collectionId, e);
    }
  }

  private void logCaughtEmailException(Integer collectionId, Exception e) {
    logException("Exception sending email for collection id: " + collectionId, e);
  }

  private DataAccessRequest createProgressReportWithDocuments(
      User user,
      ContainerRequest request,
      DataAccessRequest progressReport,
      DataAccessRequest parentDar,
      ProgressReportDocumentUploads documentUploads)
      throws IOException {
    // GCS cannot participate in the database transaction. Treat the upload and transactional
    // create as one application-level unit by compensating for any uploaded blobs if either fails.
    try {
      populateProgressReportWithDocuments(
          user,
          documentUploads.collaborationInputStream(),
          documentUploads.collaborationFileDetails(),
          documentUploads.ethicsInputStream(),
          documentUploads.ethicsFileDetails(),
          progressReport,
          parentDar);
      return dataAccessRequestService.createProgressReport(
          user, progressReport, parentDar, request);
    } catch (IOException | RuntimeException e) {
      rollbackProgressReportDocuments(progressReport, user.getUserId());
      throw e;
    }
  }

  private record ProgressReportDocumentUploads(
      InputStream collaborationInputStream,
      FormDataContentDisposition collaborationFileDetails,
      InputStream ethicsInputStream,
      FormDataContentDisposition ethicsFileDetails) {}

  public void populateProgressReportWithDocuments(
      User user,
      InputStream collabInputStream,
      FormDataContentDisposition collabFileDetails,
      InputStream ethicsInputStream,
      FormDataContentDisposition ethicsFileDetails,
      DataAccessRequest childDar,
      DataAccessRequest parentDar)
      throws IOException {
    // Closing grants does not authorize further data use, so dataset-dependent document
    // requirements do not apply. In particular, client selection must not control validation.
    if (childDar.getIsCloseoutProgressReport()) {
      return;
    }
    // Each document is uploaded once, however many datasets require it: the request's stream can
    // only be read once.
    boolean uploadCollab = false;
    boolean uploadEthics = false;
    boolean needsCollab = false;
    boolean needsEthics = false;
    for (Integer datasetId : childDar.getDatasetIds()) {
      Dataset dataset = datasetService.findDatasetById(user, datasetId);
      if (dataset == null) {
        throw new NotFoundException("Dataset " + datasetId + " not found");
      }
      DataUse dataUse = dataset.getDataUse();
      if (dataUse == null) {
        throw new BadRequestException("Dataset " + datasetId + " is missing data use(s)");
      }
      if (Boolean.TRUE.equals(dataUse.getCollaboratorRequired())) {
        String parentCollabLocation = parentDar.getData().getCollaborationLetterLocation();
        if (!fileSent(collabFileDetails) && Strings.isNullOrEmpty(parentCollabLocation)) {
          throw new BadRequestException("Collaboration document is required");
        }
        // Without a new file, the parent's document satisfies the requirement.
        needsCollab = true;
        uploadCollab = fileSent(collabFileDetails);
      }
      if (Boolean.TRUE.equals(dataUse.getEthicsApprovalRequired())) {
        String parentEthicsLocation = parentDar.getData().getIrbDocumentLocation();
        if (!fileSent(ethicsFileDetails) && Strings.isNullOrEmpty(parentEthicsLocation)) {
          throw new BadRequestException("Ethics approval document is required");
        }
        needsEthics = true;
        uploadEthics = fileSent(ethicsFileDetails);
      }
    }
    // The report's document locations start empty, so these uploads replace nothing. Without a
    // new file the report gets its own copy of the parent's document, so it can serve it and a
    // later renewal finds it, and a failed report deletes only its copy.
    if (uploadCollab) {
      uploadDocumentContents(
          DarDocumentType.COLLABORATION,
          childDar,
          requireContent(collabInputStream, "Collaboration"),
          collabFileDetails);
    } else if (needsCollab) {
      copyParentDocument(DarDocumentType.COLLABORATION, childDar, parentDar);
    }
    if (uploadEthics) {
      uploadDocumentContents(
          DarDocumentType.IRB,
          childDar,
          requireContent(ethicsInputStream, "Ethics approval"),
          ethicsFileDetails);
    } else if (needsEthics) {
      copyParentDocument(DarDocumentType.IRB, childDar, parentDar);
    }
  }

  /**
   * Whether the request carries a file for this part. duos-ui sends an empty part for a document it
   * has no file for, which still arrives with details but no file name; browsers don't send a size.
   */
  private static boolean fileSent(FormDataContentDisposition fileDetails) {
    return fileDetails != null && !Strings.isNullOrEmpty(fileDetails.getFileName());
  }

  /**
   * An empty file doesn't satisfy a required document. Browsers don't send a size, so the first
   * byte is read to check, then put back for the upload.
   */
  private static InputStream requireContent(InputStream content, String document)
      throws IOException {
    PushbackInputStream stream =
        new PushbackInputStream(content == null ? InputStream.nullInputStream() : content, 1);
    int first = stream.read();
    if (first == -1) {
      throw new BadRequestException(document + " document is empty");
    }
    stream.unread(first);
    return stream;
  }

  /** Gives a progress report its own copy of its parent's document of this type. */
  private void copyParentDocument(
      DarDocumentType type, DataAccessRequest report, DataAccessRequest parent) {
    boolean irb = DarDocumentType.IRB.equals(type);
    DataAccessRequestData parentData = parent.getData();
    String location =
        irb ? parentData.getIrbDocumentLocation() : parentData.getCollaborationLetterLocation();
    String name = irb ? parentData.getIrbDocumentName() : parentData.getCollaborationLetterName();
    BlobId copy;
    try {
      copy = gcsService.copyDocument(location, UUID.randomUUID());
    } catch (NotFoundException e) {
      throw new BadRequestException(
          "The parent request's %s document is missing; upload a new one"
              .formatted(irb ? "ethics approval" : "collaboration"));
    }
    if (irb) {
      report.getData().setIrbDocumentLocation(copy.getName());
      report.getData().setIrbDocumentName(name);
    } else {
      report.getData().setCollaborationLetterLocation(copy.getName());
      report.getData().setCollaborationLetterName(name);
    }
  }

  private void rollbackProgressReportDocuments(DataAccessRequest progressReport, Integer userId) {
    for (DarDocumentType type : DarDocumentType.values()) {
      String location = newDocumentLocation(type, progressReport);
      if (Objects.nonNull(location)) {
        deleteDarDocument(
            progressReport,
            type,
            location,
            DarDocumentDeletionReason.FAILED_REPORT_CLEANUP,
            userId);
      }
    }
  }

  @GET
  @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
  @Path("/v2/{referenceId}/collaborationDocument")
  @RolesAllowed({ADMIN, CHAIRPERSON, MEMBER, RESEARCHER})
  public Response getCollaborationDocument(
      @Auth DuosUser duosUser, @PathParam("referenceId") String referenceId) {
    try {
      DataAccessRequest dar = getDarById(referenceId);
      validateAuthedRoleUser(
          List.of(UserRoles.ADMIN, UserRoles.CHAIRPERSON, UserRoles.MEMBER), duosUser, referenceId);
      if (dar.getData() != null
          && StringUtils.isNotEmpty(dar.getData().getCollaborationLetterLocation())
          && StringUtils.isNotEmpty(dar.getData().getCollaborationLetterName())) {
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
  @UploadSizeLimited
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/v2/{referenceId}/collaborationDocument")
  @RolesAllowed({RESEARCHER})
  public Response uploadCollaborationDocument(
      @Auth DuosUser duosUser,
      @PathParam("referenceId") String referenceId,
      @FormDataParam("file") InputStream uploadInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    try {
      User user = duosUser.getUser();
      DataAccessRequest dar = getDarById(referenceId);
      checkAuthorizedUpdateUser(user, dar);
      DataAccessRequest updatedDar =
          updateDarWithDocumentContents(
              DarDocumentType.COLLABORATION, user, dar, uploadInputStream, fileDetail);
      return Response.ok(updatedDar.convertToSimplifiedDar()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @Path("/v2/{referenceId}")
  @Produces("application/json")
  @RolesAllowed({ADMIN, RESEARCHER})
  public Response deleteDar(@Auth DuosUser duosUser, @PathParam("referenceId") String referenceId) {
    try {
      DataAccessRequest dataAccessRequest =
          validateAuthedRoleUser(Collections.singletonList(UserRoles.ADMIN), duosUser, referenceId);
      dataAccessRequestService.deleteDataAccessRequest(dataAccessRequest);
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  private DataAccessRequest populateDarFromJsonString(User user, String json) {
    DataAccessRequest newDar = new DataAccessRequest();
    DataAccessRequestData data = DataAccessRequestData.populateDARData(json);
    // Only the server sets a DAR's status. A new draft or a submission (even of an existing draft)
    // never starts with one.
    data.setStatus(null);
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

  /**
   * Only the server sets a DAR's status (researcher cancel) and its document fields (the upload
   * endpoints), so a draft edit keeps the stored ones; a client can only remove a document. Returns
   * the keys the update takes from the stored row.
   */
  private static List<String> keepServerManagedFields(
      DataAccessRequestData data, DataAccessRequest stored) {
    data.setStatus(Objects.nonNull(stored.getData()) ? stored.getData().getStatus() : null);
    return Stream.concat(Stream.of("status"), data.keepStoredDocuments(stored.getData()).stream())
        .toList();
  }

  private void checkAuthorizedUpdateUser(User user, DataAccessRequest dar) {
    if (!user.getUserId().equals(dar.getUserId())) {
      throw new ForbiddenException("User not authorized to update this Data Access Request");
    }
    if (user.getLibraryCard() == null) {
      throw new LibraryCardRequiredException();
    }
  }

  private DataAccessRequest updateDarWithDocumentContents(
      DarDocumentType type,
      User user,
      DataAccessRequest dar,
      InputStream uploadInputStream,
      FormDataContentDisposition fileDetail)
      throws IOException {
    // When we move updateDarWithDocumentContents to the service tier, we should incorporate the
    // code below into that method
    if (!dar.getDraft()) {
      throw new SubmittedDARCannotBeEditedException();
    }
    String previousLocation = uploadDocumentContents(type, dar, uploadInputStream, fileDetail);
    DocumentSave saved;
    try {
      // Saves only this document's fields; returns the location it replaced, read under the lock.
      saved = dataAccessRequestService.saveDocument(user, dar, type);
    } catch (RuntimeException e) {
      // Usually nothing was saved, but a failure can also follow a commit. Check which document
      // the stored DAR points to before deleting anything.
      reconcileFailedSave(type, dar, newDocumentLocation(type, dar), previousLocation);
      throw e;
    }
    if (Objects.nonNull(saved.previousLocation())) {
      deleteDarDocument(
          dar, type, saved.previousLocation(), DarDocumentDeletionReason.REPLACED, dar.getUserId());
    }
    return saved.dar();
  }

  private void reconcileFailedSave(
      DarDocumentType type, DataAccessRequest dar, String uploadLocation, String previousLocation) {
    DataAccessRequest stored;
    try {
      stored = dataAccessRequestService.findByReferenceId(dar.getReferenceId());
    } catch (NotFoundException e) {
      // The DAR is gone, so nothing refers to the upload.
      deleteDarDocument(
          dar,
          type,
          uploadLocation,
          DarDocumentDeletionReason.FAILED_SAVE_CLEANUP,
          dar.getUserId());
      return;
    } catch (RuntimeException e) {
      // Can't tell whether the DAR refers to it: an orphaned blob is better than a missing one.
      logWarn(
          "Kept upload %s for DAR %s: %s"
              .formatted(uploadLocation, dar.getReferenceId(), e.getMessage()));
      return;
    }
    if (stored.getData() != null && uploadLocation.equals(newDocumentLocation(type, stored))) {
      // The save committed: the previous document is the one nothing refers to now.
      if (Objects.nonNull(previousLocation)) {
        deleteDarDocument(
            dar, type, previousLocation, DarDocumentDeletionReason.REPLACED, dar.getUserId());
      }
    } else {
      deleteDarDocument(
          dar,
          type,
          uploadLocation,
          DarDocumentDeletionReason.FAILED_SAVE_CLEANUP,
          dar.getUserId());
    }
  }

  private static String newDocumentLocation(DarDocumentType type, DataAccessRequest dar) {
    return DarDocumentType.IRB.equals(type)
        ? dar.getData().getIrbDocumentLocation()
        : dar.getData().getCollaborationLetterLocation();
  }

  /**
   * Uploads the document contents to GCS and mutates the dar data object with the blobId.
   *
   * @param type The type of document (IRB or Collaboration)
   * @param dar The Data Access Request
   * @param uploadInputStream The input stream of the file to be uploaded
   * @param fileDetail The file details
   * @return the location of the document this one replaces, or null. It is not deleted here.
   * @throws IOException if an error occurs during upload
   */
  public String uploadDocumentContents(
      DarDocumentType type,
      DataAccessRequest dar,
      InputStream uploadInputStream,
      FormDataContentDisposition fileDetail)
      throws IOException {
    // This should be moved to service tier logic and the transactions should be coordinated
    validateFileDetails(fileDetail);
    String fileName = fileDetail.getFileName();
    UUID id = UUID.randomUUID();
    BlobId blobId = gcsService.storeDocument(uploadInputStream, fileDetail.getType(), id);
    String previousLocation = null;
    switch (type) {
      case IRB:
        previousLocation = dar.getData().getIrbDocumentLocation();
        dar.getData().setIrbDocumentLocation(blobId.getName());
        dar.getData().setIrbDocumentName(fileName);
        break;
      case COLLABORATION:
        previousLocation = dar.getData().getCollaborationLetterLocation();
        dar.getData().setCollaborationLetterLocation(blobId.getName());
        dar.getData().setCollaborationLetterName(fileName);
        break;
      default:
        break;
    }
    // The caller deletes the previous document once the DAR no longer refers to it.
    return previousLocation;
  }

  /**
   * Deletes a DAR document's file and records the deletion, unless something else still refers to
   * the file. The DAR's owner is recorded: only the owner can upload, save or file a report.
   */
  private void deleteDarDocument(
      DataAccessRequest dar,
      DarDocumentType type,
      String blobIdName,
      DarDocumentDeletionReason reason,
      Integer userId) {
    try {
      // A location planted before document fields were server-managed can name a file that
      // another DAR or a file storage object (DAA, dataset or draft file) still uses.
      if (dataAccessRequestService.isDocumentReferenced(blobIdName)) {
        logWarn(
            "Kept document %s of DAR %s: still referenced"
                .formatted(blobIdName, dar.getReferenceId()));
        return;
      }
      if (gcsService.deleteDocument(blobIdName)) {
        dataAccessRequestService.recordDocumentDeletion(
            dar.getReferenceId(), type, blobIdName, reason, userId);
      }
    } catch (Exception e) {
      String message =
          String.format(
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
   * <p>Additional restrictions apply for non-admin, non-creator users:
   *
   * <ul>
   *   <li>CHAIRPERSON / MEMBER: must belong to a DAC that governs at least one dataset on the DAR.
   *   <li>SIGNINGOFFICIAL: must share an institution with the DAR creator.
   * </ul>
   *
   * @param allowableRoles List of roles that would allow the user to access the resource
   * @param duosUser The DuosUser
   * @param referenceId The referenceId of the resource.
   * @return dataAccessRequest The data access request underlying the referenceId
   */
  private DataAccessRequest validateAuthedRoleUser(
      final List<UserRoles> allowableRoles, DuosUser duosUser, String referenceId) {
    DataAccessRequest dataAccessRequest = getDarById(referenceId);
    User user = duosUser.getUser();
    if (Objects.nonNull(dataAccessRequest.getUserId()) && dataAccessRequest.getUserId() > 0) {
      super.validateAuthedRoleUser(allowableRoles, user, dataAccessRequest.getUserId());
    } else {
      logWarn(
          String.format(
              "DataAccessRequest '%s' has an invalid userId", dataAccessRequest.getReferenceId()));
      super.validateAuthedRoleUser(allowableRoles, user, dataAccessRequest.getUserId());
    }

    boolean isCreator =
        Objects.nonNull(dataAccessRequest.getUserId())
            && dataAccessRequest.getUserId().equals(user.getUserId());
    boolean isAdmin = user.hasUserRole(UserRoles.ADMIN);

    if (!isCreator && !isAdmin) {
      if (user.hasUserRole(UserRoles.CHAIRPERSON) || user.hasUserRole(UserRoles.MEMBER)) {
        validateDacMembership(user, dataAccessRequest);
      }
      if (user.hasUserRole(UserRoles.SIGNINGOFFICIAL)) {
        validateSigningOfficialInstitution(user, dataAccessRequest);
      }
    }

    return dataAccessRequest;
  }

  /**
   * Verifies that the user is a chair or member of at least one DAC that governs a dataset
   * referenced by the given DAR.
   */
  private void validateDacMembership(User user, DataAccessRequest dar) {
    List<Integer> datasetIds = dar.getDatasetIds();
    if (CollectionUtils.isEmpty(datasetIds)) {
      throw new ForbiddenException("User does not have permission");
    }
    Set<Dac> dacsForDatasets = dacService.findByDatasetId(datasetIds);
    boolean isMember =
        dacsForDatasets.stream()
            .anyMatch(
                dac ->
                    (dac.getChairpersons() != null
                            && dac.getChairpersons().stream()
                                .anyMatch(c -> c.getUserId().equals(user.getUserId())))
                        || (dac.getMembers() != null
                            && dac.getMembers().stream()
                                .anyMatch(m -> m.getUserId().equals(user.getUserId()))));
    if (!isMember) {
      throw new ForbiddenException("User does not have permission");
    }
  }

  /** Verifies that the signing official shares an institution with the user who created the DAR. */
  private void validateSigningOfficialInstitution(User signingOfficial, DataAccessRequest dar) {
    User darCreator = userService.findUserById(dar.getUserId());
    if (darCreator == null
        || signingOfficial.getInstitutionId() == null
        || !signingOfficial.getInstitutionId().equals(darCreator.getInstitutionId())) {
      throw new ForbiddenException("User does not have permission");
    }
  }
}
