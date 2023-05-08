package org.broadinstitute.consent.http.resources;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.networknt.schema.ValidationMessage;
import io.dropwizard.auth.Auth;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.broadinstitute.consent.http.models.dto.DatasetPropertyDTO;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.DatasetRegistrationService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.util.JsonSchemaUtil;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONObject;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Path("api/dataset")
public class DatasetResource extends Resource {

    private final String END_OF_LINE = System.lineSeparator();
    private final DatasetService datasetService;
    private final DatasetRegistrationService datasetRegistrationService;
    private final UserService userService;
    private final DataAccessRequestService darService;

    private final JsonSchemaUtil jsonSchemaUtil;

    private final String defaultDataSetSampleFileName = "DataSetSample.tsv";
    private final String defaultDataSetSampleContent = "Dataset Name\tData Type\tSpecies\tPhenotype/Indication\t# of participants\tDescription\tdbGAP\tData Depositor\tPrincipal Investigator(PI)\tSample Collection ID\tConsent ID"
            + "\n(Bucienne Monco) - Muc-1 Kidney Disease\tDNA, whole genome\thuman\tmuc-1, kidney disease\t31\tmuc-1 patients that developed cancer , 5 weeks after treatment\thttp://....\tJohn Doe\tMark Smith\tSC-20658\t1";

    private String dataSetSampleFileName;
    private String dataSetSampleContent;

    void resetDataSetSampleFileName() {
        dataSetSampleFileName = defaultDataSetSampleFileName;
    }

    void resetDataSetSampleContent() {
        dataSetSampleContent = defaultDataSetSampleContent;
    }

    @Inject
    public DatasetResource(DatasetService datasetService, UserService userService, DataAccessRequestService darService, DatasetRegistrationService datasetRegistrationService) {
        this.datasetService = datasetService;
        this.userService = userService;
        this.darService = darService;
        this.datasetRegistrationService = datasetRegistrationService;
        this.jsonSchemaUtil = new JsonSchemaUtil();
        resetDataSetSampleFileName();
        resetDataSetSampleContent();
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
        List<DatasetPropertyDTO> invalidProperties = datasetService.findInvalidProperties(inputDataset.getProperties());
        if (invalidProperties.size() > 0) {
            List<String> invalidKeys = invalidProperties.stream()
                    .map(DatasetPropertyDTO::getPropertyName)
                    .collect(Collectors.toList());
            throw new BadRequestException("Dataset contains invalid properties that could not be recognized or associated with a key: " + invalidKeys.toString());
        }
        List<DatasetPropertyDTO> duplicateProperties = datasetService.findDuplicateProperties(inputDataset.getProperties());
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
            throw new ClientErrorException("Dataset name: " + name + " is already in use", Status.CONFLICT);
        }
        User dacUser = userService.findUserByEmail(authUser.getGenericUser().getEmail());
        Integer userId = dacUser.getUserId();
        try {
            DatasetDTO createdDatasetWithConsent = datasetService.createDatasetWithConsent(inputDataset, name, userId);
            URI uri = info.getRequestUriBuilder().replacePath("api/dataset/{datasetId}").build(createdDatasetWithConsent.getDataSetId());
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


            DatasetRegistrationSchemaV1 registration = jsonSchemaUtil.deserializeDatasetRegistration(json);
            User user = userService.findUserByEmail(authUser.getEmail());

            // key: field name (not file name), value: file body part
            Map<String, FormDataBodyPart> files = extractFilesFromMultiPart(multipart);

            // Generate datasets from registration
            List<Dataset> datasets = datasetRegistrationService.createDatasetsFromRegistration(
                    registration,
                    user,
                    files);

            URI uri = UriBuilder.fromPath("/api/dataset/v2").build();
            return Response.created(uri).entity(datasets).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    /**
     * Finds and validates all the files uploaded to the multipart.
     *
     * @param multipart Form data
     * @return Map of file body parts, where the key is the name of the field and the value is the body part including the file(s).
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

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/{datasetId}")
    @RolesAllowed({ADMIN, CHAIRPERSON})
    public Response updateDataset(@Auth AuthUser authUser, @Context UriInfo info, @PathParam("datasetId") Integer datasetId, String json) {
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
            List<DatasetPropertyDTO> invalidProperties = datasetService.findInvalidProperties(inputDataset.getProperties());
            if (invalidProperties.size() > 0) {
                List<String> invalidKeys = invalidProperties.stream()
                        .map(DatasetPropertyDTO::getPropertyName)
                        .collect(Collectors.toList());
                throw new BadRequestException("Dataset contains invalid properties that could not be recognized or associated with a key: " + invalidKeys.toString());
            }
            List<DatasetPropertyDTO> duplicateProperties = datasetService.findDuplicateProperties(inputDataset.getProperties());
            if (duplicateProperties.size() > 0) {
                throw new BadRequestException("Dataset contains multiple values for the same property.");
            }
            User user = userService.findUserByEmail(authUser.getGenericUser().getEmail());
            // Validate that the admin/chairperson has edit access to this dataset
            validateDatasetDacAccess(user, datasetExists);
            Integer userId = user.getUserId();
            Optional<Dataset> updatedDataset = datasetService.updateDataset(inputDataset, datasetId, userId);
            if (updatedDataset.isPresent()) {
                URI uri = info.getRequestUriBuilder().replacePath("api/dataset/{datasetId}").build(updatedDataset.get().getDataSetId());
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
    public Response describeDataSets(@Auth AuthUser authUser) {
        try {
            User user = userService.findUserByEmail(authUser.getEmail());
            Collection<DatasetDTO> dataSetList = datasetService.describeDatasets(user.getUserId());
            return Response.ok(dataSetList, MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Produces("application/json")
    @PermitAll
    @Path("/v2")
    public Response findAllDatasetsAvailableToUser(@Auth AuthUser authUser) {
        try {
            User user = userService.findUserByEmail(authUser.getEmail());
            List<Dataset> datasets = datasetService.findAllDatasetsByUser(user);
            return Response.ok(datasets).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Deprecated // Use /v2/{datasetId}
    @Path("/{datasetId}")
    @Produces("application/json")
    @PermitAll
    public Response describeDataSet(@PathParam("datasetId") Integer datasetId) {
        try {
            DatasetDTO datasetDTO = datasetService.getDatasetDTO(datasetId);
            return Response.ok(datasetDTO, MediaType.APPLICATION_JSON).build();
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
    @Path("/batch")
    @Produces("application/json")
    @PermitAll
    public Response getDatasets(@QueryParam("ids") List<Integer> datasetIds) {
        try {
            List<Dataset> datasets = datasetService.findDatasetsByIds(datasetIds);

            Set<Integer> foundIds = datasets.stream().map(Dataset::getDataSetId).collect(Collectors.toSet());
            if (!foundIds.containsAll(datasetIds)) {
                // find the differences
                List<Integer> differences = new ArrayList<>(datasetIds);
                differences.removeAll(foundIds);
                throw new NotFoundException(
                        "Could not find datasets with ids: "
                                + String.join(",", differences.stream().map((i) -> i.toString()).collect(Collectors.toSet())));

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
    public Response findAllActiveStudyNames() {
        try {
            Set<String> studyNames = datasetService.findAllActiveStudyNames();
            return Response.ok(studyNames).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Path("/sample")
    @PermitAll
    public Response getDataSetSample() {
        String msg = "GETting Data Set Sample";
        logDebug(msg);
        InputStream inputStream = null;
        try {
            inputStream = new ByteArrayInputStream(dataSetSampleContent.getBytes());
        } catch (Exception e) {
            logException("Error when GETting dataset sample.", e);
            return createExceptionResponse(e);
        }
        return Response.ok(inputStream).header("Content-Disposition", "attachment; filename=" + dataSetSampleFileName).build();
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

            JSONObject json = new JSONObject();

            Collection<Dictionary> headers = datasetService.describeDictionaryByReceiveOrder();

            StringBuilder sb = new StringBuilder();
            String TSV_DELIMITER = "\t";
            for (Dictionary header : headers) {
                if (sb.length() > 0)
                    sb.append(TSV_DELIMITER);
                sb.append(header.getKey());
            }
            sb.append(END_OF_LINE);

            if (CollectionUtils.isEmpty(idList)) {
                json.put("datasets", sb.toString());
                return Response.ok(json.toString(), MediaType.APPLICATION_JSON).build();
            }

            Collection<DatasetDTO> rows = datasetService.describeDataSetsByReceiveOrder(idList);

            for (DatasetDTO row : rows) {
                StringBuilder sbr = new StringBuilder();
                DatasetPropertyDTO property = new DatasetPropertyDTO("Consent ID", row.getConsentId());
                List<DatasetPropertyDTO> props = row.getProperties();
                props.add(property);
                for (DatasetPropertyDTO prop : props) {
                    if (sbr.length() > 0)
                        sbr.append(TSV_DELIMITER);
                    sbr.append(prop.getPropertyValue());
                }
                sbr.append(END_OF_LINE);
                sb.append(sbr);
            }
            String tsv = sb.toString();

            json.put("datasets", tsv);
            return Response.ok(json.toString(), MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{datasetId}")
    @RolesAllowed({ADMIN, CHAIRPERSON})
    public Response delete(@Auth AuthUser authUser, @PathParam("datasetId") Integer datasetId, @Context UriInfo info) {
        try {
            User user = userService.findUserByEmail(authUser.getEmail());
            Dataset dataset = datasetService.findDatasetById(datasetId);
            // Validate that the admin/chairperson has edit/delete access to this dataset
            validateDatasetDacAccess(user, dataset);
            datasetService.deleteDataset(datasetId, user.getUserId());
            return Response.ok().build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/disable/{datasetId}/{active}")
    @RolesAllowed({ADMIN, CHAIRPERSON})
    public Response disableDataSet(@Auth AuthUser authUser, @PathParam("datasetId") Integer datasetId, @PathParam("active") Boolean active, @Context UriInfo info) {
        try {
            User user = userService.findUserByEmail(authUser.getEmail());
            Dataset dataset = datasetService.findDatasetById(datasetId);
            // Validate that the admin/chairperson has edit access to this dataset
            validateDatasetDacAccess(user, dataset);
            datasetService.disableDataset(datasetId, active);
            return Response.ok().build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Path("/dictionary")
    @Produces("application/json")
    @PermitAll
    public Response describeDictionary() {
        try {
            Collection<Dictionary> dictionaries = datasetService.describeDictionaryByDisplayOrder();
            return Response.ok(dictionaries).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Path("/autocomplete/{partial}")
    @Produces("application/json")
    @PermitAll
    @Deprecated
    public Response datasetAutocomplete(@Auth AuthUser authUser, @PathParam("partial") String partial) {
        try {
            User user = userService.findUserByEmail(authUser.getEmail());
            Integer userId = user.getUserId();
            List<Map<String, String>> datasets = datasetService.autoCompleteDatasets(partial, userId);
            return Response.ok(datasets, MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Path("/search")
    @Produces("application/json")
    @PermitAll
    public Response searchDatasets(@Auth AuthUser authUser, @QueryParam("query") String query) {
        try {
            User user = userService.findUserByEmail(authUser.getEmail());
            List<Dataset> datasets = datasetService.searchDatasets(query, user);
            return Response.ok().entity(unmarshal(datasets)).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @PUT
    @Produces("application/json")
    @RolesAllowed(ADMIN)
    public Response updateNeedsReviewDataSets(@QueryParam("dataSetId") Integer dataSetId, @QueryParam("needsApproval") Boolean needsApproval) {
        try {
            Dataset dataset = datasetService.updateNeedsReviewDatasets(dataSetId, needsApproval);
            return Response.ok().entity(unmarshal(dataset)).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @PUT
    @Produces("application/json")
    @RolesAllowed(ADMIN)
    @Path("/{id}/datause")
    public Response updateDatasetDataUse(@Auth AuthUser authUser, @PathParam("id") Integer id, String dataUseJson) {
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
            return createExceptionResponse(new BadRequestException("Invalid JSON Syntax: " + dataUseJson));
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @PermitAll
    @Path("/{datasetId}/approved/users")
    public Response downloadDatasetApprovedUsers(@Auth AuthUser authUser, @PathParam("datasetId") Integer datasetId) {
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
