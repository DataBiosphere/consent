package org.broadinstitute.consent.http.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.networknt.schema.ValidationMessage;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetStudySummary;
import org.broadinstitute.consent.http.models.DatasetSummary;
import org.broadinstitute.consent.http.models.DatasetUpdate;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.broadinstitute.consent.http.models.dto.DatasetPropertyDTO;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.DatasetRegistrationService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.ElasticSearchService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.util.JsonSchemaUtil;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;


@Path("api/dataset")
public class DatasetResource extends Resource {

  private final String END_OF_LINE = System.lineSeparator();
  private final DatasetService datasetService;
  private final DatasetRegistrationService datasetRegistrationService;
  private final UserService userService;
  private final DataAccessRequestService darService;
  private final ElasticSearchService elasticSearchService;

  private final JsonSchemaUtil jsonSchemaUtil;

  @Inject
  public DatasetResource(DatasetService datasetService, UserService userService,
      DataAccessRequestService darService, DatasetRegistrationService datasetRegistrationService,
      ElasticSearchService elasticSearchService) {
    this.datasetService = datasetService;
    this.userService = userService;
    this.darService = darService;
    this.datasetRegistrationService = datasetRegistrationService;
    this.elasticSearchService = elasticSearchService;
    this.jsonSchemaUtil = new JsonSchemaUtil();
  }

  @Deprecated
  @POST
  @Consumes("application/json")
  @Produces("application/json")
  @Path("/v2")
  @RolesAllowed({ADMIN, CHAIRPERSON})
  public Response createDataset(@Auth AuthUser authUser, @Context UriInfo info, String json) {
    DatasetDTO inputDataset = new Gson().fromJson(json, DatasetDTO.class);
    if (Objects.isNull(inputDataset)) {
      throw new BadRequestException("Dataset is required");
    }
    if (Objects.isNull(inputDataset.getProperties()) || inputDataset.getProperties().isEmpty()) {
      throw new BadRequestException("Dataset must contain required properties");
    }
    List<DatasetPropertyDTO> invalidProperties = datasetService.findInvalidProperties(
        inputDataset.getProperties());
    if (invalidProperties.size() > 0) {
      List<String> invalidKeys = invalidProperties.stream()
          .map(DatasetPropertyDTO::getPropertyName)
          .collect(Collectors.toList());
      throw new BadRequestException(
          "Dataset contains invalid properties that could not be recognized or associated with a key: "
              + invalidKeys.toString());
    }
    List<DatasetPropertyDTO> duplicateProperties = datasetService.findDuplicateProperties(
        inputDataset.getProperties());
    if (duplicateProperties.size() > 0) {
      throw new BadRequestException("Dataset contains multiple values for the same property.");
    }
    String name = "";
    try {
      name = inputDataset.getPropertyValue("Dataset Name");
    } catch (IndexOutOfBoundsException e) {
      throw new BadRequestException("Dataset name is required");
    }
    if (Objects.isNull(name) || name.isBlank()) {
      throw new BadRequestException("Dataset name is required");
    }
    Dataset datasetNameAlreadyUsed = datasetService.getDatasetByName(name);
    if (Objects.nonNull(datasetNameAlreadyUsed)) {
      throw new ClientErrorException("Dataset name: " + name + " is already in use",
          Status.CONFLICT);
    }
    User dacUser = userService.findUserByEmail(authUser.getEmail());
    Integer userId = dacUser.getUserId();
    try {
      DatasetDTO createdDatasetWithConsent = datasetService.createDatasetFromDatasetDTO(
          inputDataset,
          name, userId);
      URI uri = info.getRequestUriBuilder().replacePath("api/dataset/{datasetId}")
          .build(createdDatasetWithConsent.getDataSetId());
      return Response.created(uri).entity(createdDatasetWithConsent).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Consumes({MediaType.MULTIPART_FORM_DATA})
  @Produces({MediaType.APPLICATION_JSON})
  @Path("/v3")
  @RolesAllowed({ADMIN, CHAIRPERSON, DATASUBMITTER})
  /*
   * This endpoint accepts a json instance of a dataset-registration-schema_v1.json schema.
   * With that object, we can fully create datasets from the provided values.
   */
  public Response createDatasetRegistration(
      @Auth AuthUser authUser,
      FormDataMultiPart multipart,
      @FormDataParam("dataset") String json) {
    try {
      Set<ValidationMessage> errors = jsonSchemaUtil.validateSchema_v1(json);
      if (!errors.isEmpty()) {
        throw new BadRequestException(
            "Invalid schema:\n"
                + String.join("\n", errors.stream().map(ValidationMessage::getMessage).toList()));
      }

      DatasetRegistrationSchemaV1 registration = jsonSchemaUtil.deserializeDatasetRegistration(
          json);
      User user = userService.findUserByEmail(authUser.getEmail());

      // key: field name (not file name), value: file body part
      Map<String, FormDataBodyPart> files = extractFilesFromMultiPart(multipart);

      // Generate datasets from registration
      List<Dataset> datasets = datasetRegistrationService.createDatasetsFromRegistration(
          registration,
          user,
          files);
      Study study = datasets.get(0).getStudy();
      DatasetRegistrationSchemaV1Builder builder = new DatasetRegistrationSchemaV1Builder();
      DatasetRegistrationSchemaV1 createdRegistration = builder.build(study, datasets);
      URI uri = UriBuilder.fromPath(String.format("/api/dataset/study/%s", study.getStudyId()))
          .build();
      String entity = GsonUtil.buildGsonNullSerializer().toJson(createdRegistration);
      return Response.created(uri).entity(entity).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  /**
   * Finds and validates all the files uploaded to the multipart.
   *
   * @param multipart Form data
   * @return Map of file body parts, where the key is the name of the field and the value is the
   * body part including the file(s).
   */
  private Map<String, FormDataBodyPart> extractFilesFromMultiPart(FormDataMultiPart multipart) {
    if (Objects.isNull(multipart)) {
      return Map.of();
    }

    Map<String, FormDataBodyPart> files = new HashMap<>();
    for (List<FormDataBodyPart> parts : multipart.getFields().values()) {
      for (FormDataBodyPart part : parts) {
        if (Objects.nonNull(part.getContentDisposition().getFileName())) {
          validateFileDetails(part.getContentDisposition());
          files.put(part.getName(), part);
        }
      }
    }

    return files;
  }

  /**
   * This endpoint updates the dataset.
   */
  @PUT
  @Consumes({MediaType.MULTIPART_FORM_DATA})
  @Produces({MediaType.APPLICATION_JSON})
  @Path("/v3/{datasetId}")
  @RolesAllowed({ADMIN, CHAIRPERSON})
  public Response updateByDatasetUpdate(
      @Auth AuthUser authUser,
      @PathParam("datasetId") Integer datasetId,
      FormDataMultiPart multipart,
      @FormDataParam("dataset") String json) {

    try {
      if (json == null || json.isEmpty()) {
        throw new BadRequestException("Dataset is required");
      }
      DatasetUpdate update = new DatasetUpdate(json);

      Dataset datasetExists = datasetService.findDatasetById(datasetId);
      if (Objects.isNull(datasetExists)) {
        throw new NotFoundException("Could not find the dataset with id: " + datasetId);
      }

      User user = userService.findUserByEmail(authUser.getEmail());

      // key: field name (not file name), value: file body part
      Map<String, FormDataBodyPart> files = extractFilesFromMultiPart(multipart);

      Dataset updatedDataset = datasetRegistrationService.updateDataset(datasetId, user, update,
          files);
      return Response.ok().entity(updatedDataset).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @Consumes("application/json")
  @Produces("application/json")
  @Path("/{datasetId}")
  @RolesAllowed({ADMIN, CHAIRPERSON})
  public Response updateDataset(@Auth AuthUser authUser, @Context UriInfo info,
      @PathParam("datasetId") Integer datasetId, String json) {
    try {
      DatasetDTO inputDataset = new Gson().fromJson(json, DatasetDTO.class);
      if (Objects.isNull(inputDataset)) {
        throw new BadRequestException("Dataset is required");
      }
      if (Objects.isNull(inputDataset.getProperties()) || inputDataset.getProperties().isEmpty()) {
        throw new BadRequestException("Dataset must contain required properties");
      }
      Dataset datasetExists = datasetService.findDatasetById(datasetId);
      if (Objects.isNull(datasetExists)) {
        throw new NotFoundException("Could not find the dataset with id: " + datasetId);
      }
      List<DatasetPropertyDTO> invalidProperties = datasetService.findInvalidProperties(
          inputDataset.getProperties());
      if (invalidProperties.size() > 0) {
        List<String> invalidKeys = invalidProperties.stream()
            .map(DatasetPropertyDTO::getPropertyName)
            .collect(Collectors.toList());
        throw new BadRequestException(
            "Dataset contains invalid properties that could not be recognized or associated with a key: "
                + invalidKeys.toString());
      }
      List<DatasetPropertyDTO> duplicateProperties = datasetService.findDuplicateProperties(
          inputDataset.getProperties());
      if (duplicateProperties.size() > 0) {
        throw new BadRequestException("Dataset contains multiple values for the same property.");
      }
      User user = userService.findUserByEmail(authUser.getEmail());
      // Validate that the admin/chairperson has edit access to this dataset
      validateDatasetDacAccess(user, datasetExists);
      Integer userId = user.getUserId();
      Optional<Dataset> updatedDataset = datasetService.updateDataset(inputDataset, datasetId,
          userId);
      if (updatedDataset.isPresent()) {
        URI uri = info.getRequestUriBuilder().replacePath("api/dataset/{datasetId}")
            .build(updatedDataset.get().getDataSetId());
        return Response.ok(uri).entity(updatedDataset.get()).build();
      } else {
        return Response.noContent().build();
      }
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces("application/json")
  @PermitAll
  @Path("/v2")
  public Response findAllDatasetsStreaming(@Auth AuthUser authUser) {
    try {
      userService.findUserByEmail(authUser.getEmail());
      StreamingOutput streamedDatasets = datasetService.findAllDatasetsAsStreamingOutput();
      return Response.ok().entity(streamedDatasets).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces("application/json")
  @PermitAll
  @Path("/v3")
  public Response findAllDatasetStudySummaries(@Auth AuthUser authUser) {
    try {
      userService.findUserByEmail(authUser.getEmail());
      List<DatasetStudySummary> summaries = datasetService.findAllDatasetStudySummaries();
      return Response.ok(summaries).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces("application/json")
  @PermitAll
  @Path("/role/{roleName}")
  public Response findDatasetsAccordingToRole(
      @Auth AuthUser authUser,
      @PathParam("roleName") String roleName) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      validateUserHasRoleName(user, roleName);
      UserRoles role = UserRoles.getUserRoleFromName(roleName);
      if (Objects.isNull(role)) {
        throw new BadRequestException("Invalid role selection: " + roleName);
      }
      List<Dataset> datasets = switch (role) {
        case ADMIN -> datasetService.findAllDatasets();
        case CHAIRPERSON -> datasetService.findDatasetsForChairperson(user);
        case DATASUBMITTER -> datasetService.findDatasetsForDataSubmitter(user);
        default -> datasetService.findPublicDatasets();
      };
      return Response.ok(datasets).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("/v2/{datasetId}")
  @Produces("application/json")
  @PermitAll
  public Response getDataset(@PathParam("datasetId") Integer datasetId) {
    try {
      Dataset dataset = datasetService.findDatasetById(datasetId);
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
  public Response getRegistrationFromDatasetIdentifier(@Auth AuthUser authUser,
      @PathParam("datasetIdentifier") String datasetIdentifier) {
    try {
      Dataset dataset = datasetService.findDatasetByIdentifier(datasetIdentifier);
      if (Objects.isNull(dataset)) {
        throw new NotFoundException(
            "No dataset exists for dataset identifier: " + datasetIdentifier);
      }
      Study study;
      if (dataset.getStudy() != null && dataset.getStudy().getStudyId() != null) {
        study = datasetService.findStudyById(dataset.getStudy().getStudyId());
      } else {
        throw new NotFoundException("No study exists for dataset identifier: " + datasetIdentifier);
      }
      DatasetRegistrationSchemaV1 registration = new DatasetRegistrationSchemaV1Builder().build(
          study, List.of(dataset));
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
  public Response getDatasets(@QueryParam("ids") List<Integer> datasetIds) {
    try {
      List<Dataset> datasets = datasetService.findDatasetsByIds(datasetIds);

      Set<Integer> foundIds = datasets.stream().map(Dataset::getDataSetId)
          .collect(Collectors.toSet());
      if (!foundIds.containsAll(datasetIds)) {
        // find the differences
        List<Integer> differences = new ArrayList<>(datasetIds)
          .stream()
          .filter(Objects::nonNull)
          .filter(Predicate.not(foundIds::contains))
          .toList();
        throw new NotFoundException(
            "Could not find datasets with ids: "
                + String.join(",",
                differences.stream().map(Object::toString).collect(Collectors.toSet())));

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
  public Response validateDatasetName(@QueryParam("name") String name) {
    try {
      Dataset datasetWithName = datasetService.getDatasetByName(name);
      return Response.ok().entity(datasetWithName.getDataSetId()).build();
    } catch (Exception e) {
      throw new NotFoundException("Could not find the dataset with name: " + name);
    }
  }

  @GET
  @Consumes("application/json")
  @Produces("application/json")
  @Path("/studyNames")
  @PermitAll
  public Response findAllStudyNames() {
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
  public Response findAllDatasetNames() {
    try {
      List<String> datasetNames = datasetService.findAllDatasetNames();
      return Response.ok(datasetNames).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Path("/download")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @PermitAll
  public Response downloadDataSets(List<Integer> idList) {
    try {
      String msg = "GETing DataSets to download";
      logDebug(msg);

      Gson gson = new Gson();
      HashMap<String, String> datasets = new HashMap<>();

      Collection<Dictionary> headers = datasetService.describeDictionaryByReceiveOrder();

      StringBuilder sb = new StringBuilder();
      String TSV_DELIMITER = "\t";
      for (Dictionary header : headers) {
        if (sb.length() > 0) {
          sb.append(TSV_DELIMITER);
        }
        sb.append(header.getKey());
      }
      sb.append(END_OF_LINE);

      if (CollectionUtils.isEmpty(idList)) {
        datasets.put("datasets", sb.toString());
        return Response.ok(gson.toJson(datasets), MediaType.APPLICATION_JSON).build();
      }

      Collection<DatasetDTO> rows = datasetService.describeDataSetsByReceiveOrder(idList);

      for (DatasetDTO row : rows) {
        StringBuilder sbr = new StringBuilder();
        DatasetPropertyDTO property = new DatasetPropertyDTO("Consent ID", row.getConsentId());
        List<DatasetPropertyDTO> props = row.getProperties();
        props.add(property);
        for (DatasetPropertyDTO prop : props) {
          if (sbr.length() > 0) {
            sbr.append(TSV_DELIMITER);
          }
          sbr.append(prop.getPropertyValue());
        }
        sbr.append(END_OF_LINE);
        sb.append(sbr);
      }
      String tsv = sb.toString();

      datasets.put("datasets", tsv);
      return Response.ok(gson.toJson(datasets), MediaType.APPLICATION_JSON).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/{datasetId}")
  @RolesAllowed({ADMIN, CHAIRPERSON, DATASUBMITTER})
  public Response delete(@Auth AuthUser authUser, @PathParam("datasetId") Integer datasetId,
      @Context UriInfo info) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      Dataset dataset = datasetService.findDatasetById(datasetId);
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
      try {
        elasticSearchService.deleteIndex(datasetId);
      } catch (IOException e) {
        logException(e);
        return createExceptionResponse(e);
      }
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Path("/index")
  @RolesAllowed(ADMIN)
  public Response indexDatasets() {
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
  public Response indexDataset(@PathParam("datasetId") Integer datasetId) {
    try {
      var dataset = datasetService.findDatasetById(datasetId);
      return elasticSearchService.indexDataset(dataset);
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @Path("/index/{datasetId}")
  @RolesAllowed(ADMIN)
  public Response deleteDatasetIndex(@PathParam("datasetId") Integer datasetId) {
    try {
      return elasticSearchService.deleteIndex(datasetId);
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("/search")
  @Produces("application/json")
  @PermitAll
  public Response searchDatasets(
      @Auth AuthUser authUser,
      @QueryParam("query") String query,
      @QueryParam("access") @DefaultValue("controlled") String accessString) {
    try {
      AccessManagement accessManagement = AccessManagement.fromValue(accessString.toLowerCase());
      User user = userService.findUserByEmail(authUser.getEmail());
      List<Dataset> datasets = datasetService.searchDatasets(query, accessManagement, user);
      return Response.ok().entity(unmarshal(datasets)).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces("application/json")
  @Path("/autocomplete")
  @PermitAll
  @Timed
  public Response autocompleteDatasets(
      @Auth AuthUser authUser,
      @QueryParam("query") String query) {
    try {
      userService.findUserByEmail(authUser.getEmail());
      List<DatasetSummary> datasets = datasetService.searchDatasetSummaries(query);
      return Response.ok(datasets).build();
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
  public Response searchDatasetIndex(@Auth AuthUser authUser, String query) {
    try {
      userService.findUserByEmail(authUser.getEmail());
      return elasticSearchService.searchDatasets(query);
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @Produces("application/json")
  @RolesAllowed(ADMIN)
  @Path("/{id}/datause")
  public Response updateDatasetDataUse(@Auth AuthUser authUser, @PathParam("id") Integer id,
      String dataUseJson) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      Gson gson = new Gson();
      DataUse dataUse = gson.fromJson(dataUseJson, DataUse.class);
      Dataset originalDataset = datasetService.findDatasetById(id);
      if (Objects.isNull(originalDataset)) {
        throw new NotFoundException("Dataset not found: " + id);
      }
      if (Objects.equals(dataUse, originalDataset.getDataUse())) {
        return Response.notModified().entity(originalDataset).build();
      }
      Dataset dataset = datasetService.updateDatasetDataUse(user, id, dataUse);
      return Response.ok().entity(dataset).build();
    } catch (JsonSyntaxException jse) {
      return createExceptionResponse(
          new BadRequestException("Invalid JSON Syntax: " + dataUseJson));
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @Produces("application/json")
  @RolesAllowed(ADMIN)
  @Path("/{id}/reprocess/datause")
  public Response syncDataUseTranslation(@Auth AuthUser authUser, @PathParam("id") Integer id) {
    try {
      Dataset ds = datasetService.syncDatasetDataUseTranslation(id);
      return Response.ok(ds).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @PermitAll
  @Path("/{datasetId}/approved/users")
  public Response downloadDatasetApprovedUsers(@Auth AuthUser authUser,
      @PathParam("datasetId") Integer datasetId) {
    try {
      String content = darService.getDatasetApprovedUsersContent(authUser, datasetId);
      return Response.ok(content)
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=DatasetApprovedUsers.tsv")
          .build();
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
    List<Integer> dacIds = user.getRoles().stream()
        .filter(r -> r.getRoleId().equals(UserRoles.CHAIRPERSON.getRoleId()))
        .map(UserRole::getDacId)
        .toList();
    if (dacIds.isEmpty()) {
      // Something went very wrong here. A chairperson with no dac ids is an error
      logWarn("Unable to find dac ids for chairperson user: " + user.getEmail());
      throw new NotFoundException();
    } else {
      if (Objects.isNull(dataset) || Objects.isNull(dataset.getDacId())) {
        logWarn("Cannot find a valid dac id for dataset: " + dataset.getDataSetId());
        throw new NotFoundException();
      } else {
        if (!dacIds.contains(dataset.getDacId())) {
          throw new NotFoundException();
        }
      }
    }
  }
}
