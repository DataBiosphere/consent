package org.broadinstitute.consent.http.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
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
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriBuilder;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.exceptions.UnprocessableEntityException;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetPatch;
import org.broadinstitute.consent.http.models.DatasetStudySummary;
import org.broadinstitute.consent.http.models.DatasetUpdate;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder;
import org.broadinstitute.consent.http.models.dto.registration.StudyRegistrationRequest;
import org.broadinstitute.consent.http.models.dto.registration.StudyRegistrationRequestValidator;
import org.broadinstitute.consent.http.service.DatasetRegistrationService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.ElasticSearchService;
import org.broadinstitute.consent.http.service.TDRService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Path("api/dataset")
public class DatasetResource extends Resource {

  private final DatasetService datasetService;
  private final DatasetRegistrationService datasetRegistrationService;
  private final TDRService tdrService;
  private final UserService userService;
  private final ElasticSearchService elasticSearchService;

  private final GCSService gcsService;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final StudyRegistrationRequestValidator createValidator =
      new StudyRegistrationRequestValidator();

  @Inject
  public DatasetResource(
      DatasetService datasetService,
      UserService userService,
      DatasetRegistrationService datasetRegistrationService,
      ElasticSearchService elasticSearchService,
      TDRService tdrService,
      GCSService gcsService) {
    this.datasetService = datasetService;
    this.userService = userService;
    this.datasetRegistrationService = datasetRegistrationService;
    this.gcsService = gcsService;
    this.elasticSearchService = elasticSearchService;
    this.tdrService = tdrService;
  }

  @POST
  @Consumes({MediaType.MULTIPART_FORM_DATA})
  @Produces({MediaType.APPLICATION_JSON})
  @Path("/v3")
  @RolesAllowed({ADMIN, CHAIRPERSON, DATASUBMITTER})
  @Timed
  /*
   * This endpoint accepts a registration payload, validated by DTO/domain validators.
   * With that object, we can fully create datasets from the provided values.
   */
  public Response createDatasetRegistration(
      @Auth DuosUser duosUser, FormDataMultiPart multipart, @FormDataParam("dataset") String json) {
    try {
      if (json == null || json.isEmpty()) {
        throw new BadRequestException("Dataset is required");
      }
      StudyRegistrationRequest request = deserializeStudyRegistrationRequest(json);
      List<String> violations = createValidator.collectViolations(request);
      if (!violations.isEmpty()) {
        String errorMessage =
            violations.stream().map(error -> " - " + error + "\n").collect(Collectors.joining());
        throw new BadRequestException("Please correct the following fields:\n" + errorMessage);
      }

      User user = duosUser.getUser();

      // key: field name (not file name), value: file body part
      Map<String, FormDataBodyPart> files = extractFilesFromMultiPart(multipart);

      // Generate datasets from registration
      List<Dataset> datasets =
          datasetRegistrationService.createDatasetsFromRegistration(request, user, files);
      Integer studyId = datasets.getFirst().getStudyId();
      Study study = datasetService.findStudy(studyId);
      DatasetRegistrationSchemaV1Builder builder = new DatasetRegistrationSchemaV1Builder();
      DatasetRegistrationSchemaV1 createdRegistration = builder.build(study, datasets);
      if (study == null) {
        Exception entityException =
            new UnprocessableEntityException("Study was not found after it was created");
        logException(entityException);
        throw entityException;
      }
      URI uri =
          UriBuilder.fromPath(String.format("/api/dataset/study/%s", study.getStudyId())).build();
      String entity = GsonUtil.buildGsonNullSerializer().toJson(createdRegistration);
      return Response.created(uri).entity(entity).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  private StudyRegistrationRequest deserializeStudyRegistrationRequest(String json) {
    StudyRegistrationRequest request;
    try {
      request = objectMapper.readValue(json, StudyRegistrationRequest.class);
    } catch (JsonProcessingException _) {
      throw new BadRequestException("Invalid registration payload");
    }
    if (request == null) {
      throw new BadRequestException("Invalid registration payload");
    }
    return request;
  }

  /** This endpoint updates the dataset. */
  @PUT
  @Consumes({MediaType.MULTIPART_FORM_DATA})
  @Produces({MediaType.APPLICATION_JSON})
  @Path("/v3/{datasetId}")
  @RolesAllowed({ADMIN, CHAIRPERSON})
  public Response updateByDatasetUpdate(
      @Auth DuosUser duosUser,
      @PathParam("datasetId") Integer datasetId,
      FormDataMultiPart multipart,
      @FormDataParam("dataset") String json) {

    try {
      if (json == null || json.isEmpty()) {
        throw new BadRequestException("Dataset is required");
      }
      DatasetUpdate update = new DatasetUpdate(json);
      User user = duosUser.getUser();
      Dataset datasetExists = datasetService.findDatasetById(user, datasetId);
      if (Objects.isNull(datasetExists)) {
        throw new NotFoundException("Could not find the dataset with id: " + datasetId);
      }

      // key: field name (not file name), value: file body part
      Map<String, FormDataBodyPart> files = extractFilesFromMultiPart(multipart);

      Dataset updatedDataset =
          datasetRegistrationService.updateDataset(datasetId, user, update, files);
      return Response.ok().entity(updatedDataset).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  /** This endpoint updates the dataset. */
  @PATCH
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Path("/{datasetId}")
  @RolesAllowed({ADMIN, CHAIRPERSON, DATASUBMITTER})
  public Response patchByDatasetUpdate(
      @Auth DuosUser duosUser, @PathParam("datasetId") Integer datasetId, String json) {
    try {
      User user = duosUser.getUser();
      Dataset existingDataset = datasetService.findDatasetById(user, datasetId);
      if (existingDataset == null) {
        throw new NotFoundException("Could not find the dataset with id: " + datasetId);
      }
      // Check permissions for non-admin roles.
      if (!user.hasUserRole(UserRoles.ADMIN)
          && !existingDataset.isCreator(user)
          && !existingDataset.isCustodian(user)) {
        throw new ForbiddenException("User does not have permission to update this dataset");
      }
      if (json == null || json.isEmpty()) {
        throw new BadRequestException("Dataset Patch is required");
      }
      Gson gson = GsonUtil.getInstance();
      DatasetPatch patch;
      try {
        patch = gson.fromJson(json, DatasetPatch.class);
      } catch (Exception _) {
        throw new BadRequestException("Unable to parse dataset patch: " + json);
      }
      if (!patch.isPatchable(existingDataset)) {
        return Response.notModified().entity(existingDataset).build();
      }
      // Validate DatasetPatch values
      List<String> existingNames = datasetService.findAllDatasetNames();
      if (patch.name() != null
          && !patch.name().equals(existingDataset.getName())
          && existingNames.contains(patch.name())) {
        throw new BadRequestException(
            "The new name for this dataset already exists: " + patch.name());
      }
      if (!patch.validateProperties()) {
        throw new BadRequestException("Properties are invalid");
      }
      Dataset patched = datasetRegistrationService.patchDataset(datasetId, user, patch);
      elasticSearchService.synchronizeDatasetInESIndex(patched, false);
      return Response.ok(patched).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces("application/json")
  @PermitAll
  @Path("/v3")
  public Response findAllDatasetStudySummaries(@Auth DuosUser duosUser) {
    try {
      List<DatasetStudySummary> summaries =
          datasetService.findAllDatasetStudySummaries(duosUser.getUser());
      return Response.ok(summaries).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("/v2/{datasetId}")
  @Produces("application/json")
  @PermitAll
  public Response getDataset(@Auth DuosUser duosUser, @PathParam("datasetId") Integer datasetId) {
    try {
      Dataset dataset = datasetService.findDatasetById(duosUser.getUser(), datasetId);
      if (Objects.isNull(dataset)) {
        throw new NotFoundException("Could not find the dataset with id: " + datasetId.toString());
      }
      return Response.ok(dataset).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("/registration/{datasetIdentifier}")
  @Produces(MediaType.APPLICATION_JSON)
  @PermitAll
  @Timed
  public Response getRegistrationFromDatasetIdentifier(
      @Auth DuosUser duosUser, @PathParam("datasetIdentifier") String datasetIdentifier) {
    try {
      User user = duosUser.getUser();
      Dataset dataset = datasetService.findDatasetByIdentifier(user, datasetIdentifier);
      if (Objects.isNull(dataset)) {
        throw new NotFoundException(
            "No dataset exists for dataset identifier: " + datasetIdentifier);
      }
      if (dataset.getStudy() == null || dataset.getStudy().getStudyId() == null) {
        throw new NotFoundException("No study exists for dataset identifier: " + datasetIdentifier);
      }
      DatasetRegistrationSchemaV1 registration =
          new DatasetRegistrationSchemaV1Builder().build(dataset.getStudy(), List.of(dataset));
      String entity = GsonUtil.buildGsonNullSerializer().toJson(registration);
      return Response.ok().entity(entity).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("/batch")
  @Produces("application/json")
  @PermitAll
  public Response getDatasets(
      @Auth DuosUser duosUser, @QueryParam("ids") List<Integer> datasetIds) {
    try {
      List<Dataset> datasets = datasetService.findDatasetsByIds(duosUser.getUser(), datasetIds);
      Set<Integer> foundIds =
          datasets.stream().map(Dataset::getDatasetId).collect(Collectors.toSet());
      if (!foundIds.containsAll(datasetIds)) {
        // find the differences
        List<Integer> differences =
            new ArrayList<>(datasetIds)
                .stream()
                    .filter(Objects::nonNull)
                    .filter(Predicate.not(foundIds::contains))
                    .toList();
        throw new NotFoundException(
            "Could not find datasets with ids: "
                + String.join(
                    ",", differences.stream().map(Object::toString).collect(Collectors.toSet())));
      }
      return Response.ok(datasets).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Consumes("application/json")
  @Produces("application/json")
  @Path("/validate")
  @PermitAll
  public Response validateDatasetName(@Auth DuosUser duosUser, @QueryParam("name") String name) {
    try {
      Dataset datasetWithName = datasetService.getDatasetByName(name);
      return Response.ok().entity(datasetWithName.getDatasetId()).build();
    } catch (Exception _) {
      throw new NotFoundException("Could not find the dataset with name: " + name);
    }
  }

  @GET
  @Consumes("application/json")
  @Produces("application/json")
  @Path("/studyNames")
  @PermitAll
  public Response findAllStudyNames(@Auth DuosUser duosUser) {
    try {
      Set<String> studyNames = datasetService.findAllStudyNames();
      return Response.ok(studyNames).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Consumes("application/json")
  @Produces("application/json")
  @Path("/datasetNames")
  @PermitAll
  public Response findAllDatasetNames(@Auth DuosUser duosUser) {
    try {
      List<String> datasetNames = datasetService.findAllDatasetNames();
      return Response.ok(datasetNames).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/{datasetId}")
  @RolesAllowed({ADMIN, CHAIRPERSON, DATASUBMITTER})
  public Response delete(@Auth DuosUser duosUser, @PathParam("datasetId") Integer datasetId) {
    try {
      User user = duosUser.getUser();
      Dataset dataset = datasetService.findDatasetById(user, datasetId);
      if (Objects.nonNull(dataset.getDeletable()) && !dataset.getDeletable()) {
        throw new BadRequestException("Dataset is in use and cannot be deleted.");
      }
      // Validate that the admin/chairperson/data submitter has edit/delete access to this dataset
      validateDatasetDacAccess(user, dataset);
      try {
        datasetService.deleteDataset(datasetId, user.getUserId());
      } catch (Exception e) {
        logException(e);
        return createExceptionResponse(e);
      }
      try (var deleteResponse = elasticSearchService.deleteIndex(datasetId, user.getUserId())) {
        if (!HttpStatusCodes.isSuccess(deleteResponse.getStatus())) {
          logWarn("Unable to delete index for dataset: " + datasetId);
        }
      }
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Path("/index")
  @RolesAllowed(ADMIN)
  public Response indexDatasets(@Auth DuosUser duosUser) {
    try {
      var datasetIds = datasetService.findAllDatasetIds();
      StreamingOutput indexResponse = elasticSearchService.indexDatasetIds(datasetIds);
      return Response.ok(indexResponse, MediaType.APPLICATION_JSON).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Path("/index/{datasetId}")
  @RolesAllowed(ADMIN)
  public Response indexDataset(@Auth DuosUser duosUser, @PathParam("datasetId") Integer datasetId) {
    try {
      return elasticSearchService.indexDataset(datasetId);
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @Path("/index/{datasetId}")
  @RolesAllowed(ADMIN)
  public Response deleteDatasetIndex(
      @Auth DuosUser duosUser, @PathParam("datasetId") Integer datasetId) {
    try {
      User user = duosUser.getUser();
      return elasticSearchService.deleteIndex(datasetId, user.getUserId());
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Path("/search/index")
  @Consumes("application/json")
  @Produces("application/json")
  @PermitAll
  @Timed
  public Response searchDatasetIndex(@Auth DuosUser duosUser, String query) {
    try {
      return elasticSearchService.searchDatasets(query);
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Path("/search/index/v2")
  @Consumes("application/json")
  @Produces("application/json")
  @PermitAll
  @Timed
  public Response searchDatasetIndexStream(@Auth DuosUser duosUser, String query) {
    try {
      InputStream inputStream = elasticSearchService.searchDatasetsStream(query);
      StreamingOutput stream = createStreamingOutput(inputStream);
      return Response.ok(stream).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @Produces("application/json")
  @RolesAllowed(ADMIN)
  @Path("/{id}/datause")
  public Response updateDatasetDataUse(
      @Auth DuosUser duosUser, @PathParam("id") Integer id, String dataUseJson) {
    try {
      User user = duosUser.getUser();
      // TODO: Replace new Gson() with GsonUtil.buildGson() — deferred pending Gson configuration
      // investigation
      Gson gson = new Gson();
      DataUse dataUse = gson.fromJson(dataUseJson, DataUse.class);
      Dataset originalDataset = datasetService.findDatasetById(user, id);
      if (Objects.isNull(originalDataset)) {
        throw new NotFoundException("Dataset not found: " + id);
      }
      if (Objects.equals(dataUse, originalDataset.getDataUse())) {
        return Response.notModified().entity(originalDataset).build();
      }
      Dataset dataset = datasetService.updateDatasetDataUse(user, id, dataUse);
      return Response.ok().entity(dataset).build();
    } catch (JsonSyntaxException _) {
      return createExceptionResponse(
          new BadRequestException("Invalid JSON Syntax: " + dataUseJson));
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  private void validateDatasetDacAccess(User user, Dataset dataset) {
    if (user.hasUserRole(UserRoles.ADMIN)) {
      return;
    }
    if (user.hasUserRole(UserRoles.DATASUBMITTER)) {
      if (dataset.getCreateUserId().equals(user.getUserId())) {
        return;
      }
      // If the user doesn't have any other appropriate role, we can return an error here,
      // otherwise, continue checking if the user has chair permissions
      if (!user.hasUserRole(UserRoles.CHAIRPERSON)) {
        logWarn("User does not have permission to delete dataset: " + user.getEmail());
        throw new NotFoundException();
      }
    }
    List<Integer> dacIds =
        user.getRoles().stream()
            .filter(r -> r.getRoleId().equals(UserRoles.CHAIRPERSON.getRoleId()))
            .map(UserRole::getDacId)
            .toList();
    if (dacIds.isEmpty()) {
      // Something went very wrong here. A chairperson with no dac ids is an error
      logWarn("Unable to find dac ids for chairperson user: " + user.getEmail());
      throw new NotFoundException();
    } else {
      if (Objects.isNull(dataset) || Objects.isNull(dataset.getDacId())) {
        logWarn("Cannot find a valid dac id for dataset: " + dataset.getDatasetId());
        throw new NotFoundException();
      } else {
        if (!dacIds.contains(dataset.getDacId())) {
          throw new NotFoundException();
        }
      }
    }
  }

  @GET
  @Produces("application/json")
  @RolesAllowed(ADMIN)
  @Path("/{id}/authorizedAccessReaders")
  public Response getAuthorizedReaders(@Auth DuosUser duosUser, @PathParam("id") Long id) {
    try {
      return Response.ok(datasetService.getAuthorizationReaders(id)).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @Produces("application/json")
  @RolesAllowed(ADMIN)
  @Path("/{id}/authorizedAccessReaders/{userId}")
  public Response addAuthorizedReaders(
      @Auth DuosUser duosUser, @PathParam("id") long datasetId, @PathParam("userId") int userId) {
    try {
      User targetUser = userService.findUserById(userId);
      if (targetUser == null || !targetUser.hasUserRole(UserRoles.RESEARCHER)) {
        return Response.status(Status.CONFLICT).build();
      }
      return Response.ok(
              datasetService.addAuthorizedReader(datasetId, userId, duosUser.getUser().getUserId()))
          .build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @Produces("application/json")
  @RolesAllowed(ADMIN)
  @Path("/{id}/authorizedAccessReaders/{userId}")
  public Response removeAuthorizedReaders(
      @Auth DuosUser duosUser, @PathParam("id") long datasetId, @PathParam("userId") long userId) {
    try {
      datasetService.removeAuthorizedAccessReader(datasetId, userId);
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces("application/json")
  @RolesAllowed(RESEARCHER)
  @Path("/{identifier}/approvedUsers")
  public Response getApprovedUsers(
      @Auth DuosUser duosUser, @PathParam("identifier") String datasetIdentifier) {
    try {
      Dataset dataset =
          datasetService.findMinimalDatasetByIdentifier(
              duosUser.getUser(), datasetIdentifier, false);
      if (Objects.isNull(dataset)) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      if (!datasetService.isAuthorizedToListUsers(
          dataset.getDatasetId(), duosUser.getUser().getUserId())) {
        return Response.status(Response.Status.FORBIDDEN).build();
      }
      return Response.ok(tdrService.getApprovedUsersForDataset(duosUser, dataset)).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("/{id}/nihInstitutionalCertification")
  @RolesAllowed({ADMIN, DATASUBMITTER, CHAIRPERSON, MEMBER})
  public Response getNihInstitutionalCertification(
      @Auth DuosUser duosUser, @PathParam("id") Integer id) {
    try {
      User requestingUser = duosUser.getUser();
      Dataset dataset = datasetService.findDatasetById(requestingUser, id);
      if (dataset == null) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      FileStorageObject nihFile = dataset.getNihInstitutionalCertificationFile();
      if (nihFile != null
          && !nihFile.getDeleted()
          && nihFile.getFileName() != null
          && nihFile.getBlobId() != null
          && (requestingUser.hasUserRole(UserRoles.ADMIN)
              || dataset.isCreator(requestingUser)
              || dataset.isCustodian(requestingUser)
              || requestingUser.verifyDACRole(CHAIRPERSON, dataset.getDacId())
              || requestingUser.verifyDACRole(MEMBER, dataset.getDacId()))) {
        InputStream fileStream = gcsService.getDocument(nihFile.getBlobId());
        StreamingOutput streamOutput = createStreamingOutput(fileStream);
        return Response.ok(streamOutput)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                String.format("attachment; filename=\"%s\"", nihFile.getFileName()))
            .build();
      } else {
        return Response.status(Status.NOT_FOUND).build();
      }
    } catch (Exception e) {
      logWarn(e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @PUT
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("/cleanupEmptyCertificationAndAlternativeSharingFiles")
  @RolesAllowed({ADMIN})
  public Response cleanupEmptyFiles(@Auth DuosUser duosUser) {
    User user = duosUser.getUser();
    datasetRegistrationService.asyncCleanupDatasetsAndStudiesWithEmptyFiles(user);
    return Response.status(Status.OK).build();
  }
}
