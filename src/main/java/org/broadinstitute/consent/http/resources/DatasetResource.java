package org.broadinstitute.consent.http.resources;

import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
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
import javax.ws.rs.core.UriInfo;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;
import org.broadinstitute.consent.http.models.dto.DataSetPropertyDTO;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.AbstractDataAccessRequestAPI;
import org.broadinstitute.consent.http.service.DataAccessRequestAPI;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.UserService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("api/dataset")
public class DatasetResource extends Resource {

    private final String END_OF_LINE = System.lineSeparator();
    private final DatasetService datasetService;
    private final DataAccessRequestAPI dataAccessRequestAPI;
    private final UserService userService;

    @Inject
    public DatasetResource(DatasetService datasetService, UserService userService) {
        this.dataAccessRequestAPI = AbstractDataAccessRequestAPI.getInstance();
        this.datasetService = datasetService;
        this.userService = userService;
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/v2")
    @RolesAllowed({ADMIN, CHAIRPERSON})
    public Response createDataset(@Auth AuthUser authUser, @Context UriInfo info, String json) {
        DataSetDTO inputDataset = new Gson().fromJson(json, DataSetDTO.class);
        if (Objects.isNull(inputDataset)) {
            throw new BadRequestException("Dataset is required");
        }
        if (Objects.isNull(inputDataset.getProperties()) || inputDataset.getProperties().isEmpty()) {
            throw new BadRequestException("Dataset must contain required properties");
        }
        List<DataSetPropertyDTO> invalidProperties = datasetService.findInvalidProperties(inputDataset.getProperties());
        if (invalidProperties.size() > 0) {
            List<String> invalidKeys = invalidProperties.stream()
                .map(DataSetPropertyDTO::getPropertyName)
                .collect(Collectors.toList());
            throw new BadRequestException("Dataset contains invalid properties that could not be recognized or associated with a key: " + invalidKeys.toString());
        }
        List<DataSetPropertyDTO> duplicateProperties = datasetService.findDuplicateProperties(inputDataset.getProperties());
        if (duplicateProperties.size() > 0) {
            throw new BadRequestException("Dataset contains multiple values for the same property.");
        }
        String name = inputDataset.getPropertyValue("Dataset Name");
        if (Objects.isNull(name)) {
            throw new BadRequestException("Dataset name is required");
        }
        DataSet datasetNameAlreadyUsed = datasetService.getDatasetByName(name);
        if (Objects.nonNull(datasetNameAlreadyUsed)) {
            throw new ClientErrorException("Dataset name: " + name + " is already in use", Status.CONFLICT);
        }
        User dacUser = userService.findUserByEmail(authUser.getGoogleUser().getEmail());
        Integer userId = dacUser.getDacUserId();

        DataSetDTO createdDataset = null;
        Consent createdConsent = null;
        try {
            createdDataset = datasetService.createDataset(inputDataset, name, userId);
            createdDataset.setDataUse(inputDataset.getDataUse());
            createdDataset.setDacId(inputDataset.getDacId());
            createdConsent = datasetService.createConsentForDataset(createdDataset);
            DataSetDTO createdDatasetWithConsent = datasetService.getDatasetDTO(createdDataset.getDataSetId());
            URI uri = info.getRequestUriBuilder().replacePath("api/dataset/{datasetId}").build(createdDatasetWithConsent.getDataSetId());
            return Response.created(uri).entity(createdDatasetWithConsent).build();
        }
        catch (Exception e) {
            if (Objects.isNull(createdConsent) && Objects.nonNull(createdDataset)) {
                datasetService.deleteDataset(createdDataset.getDataSetId());
            }
            return Response.serverError().entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/{datasetId}")
    @RolesAllowed({ADMIN, CHAIRPERSON})
    public Response updateDataset(@Auth AuthUser authUser, @Context UriInfo info, @PathParam("datasetId") Integer datasetId, String json) {
        DataSetDTO inputDataset = new Gson().fromJson(json, DataSetDTO.class);
        if (Objects.isNull(inputDataset)) {
            throw new BadRequestException("Dataset is required");
        }
        if (Objects.isNull(inputDataset.getProperties()) || inputDataset.getProperties().isEmpty()) {
            throw new BadRequestException("Dataset must contain required properties");
        }
        DataSet datasetExists = datasetService.findDatasetById(datasetId);
        if (Objects.isNull(datasetExists)) {
            throw new NotFoundException("Could not find the dataset with id: " + datasetId);
        }
        List<DataSetPropertyDTO> invalidProperties = datasetService.findInvalidProperties(inputDataset.getProperties());
        if (invalidProperties.size() > 0) {
            List<String> invalidKeys = invalidProperties.stream()
                .map(DataSetPropertyDTO::getPropertyName)
                .collect(Collectors.toList());
            throw new BadRequestException("Dataset contains invalid properties that could not be recognized or associated with a key: " + invalidKeys.toString());
        }
        List<DataSetPropertyDTO> duplicateProperties = datasetService.findDuplicateProperties(inputDataset.getProperties());
        if (duplicateProperties.size() > 0) {
            throw new BadRequestException("Dataset contains multiple values for the same property.");
        }
        User dacUser = userService.findUserByEmail(authUser.getGoogleUser().getEmail());
        Integer userId = dacUser.getDacUserId();
        Optional<DataSet> updatedDataset = datasetService.updateDataset(inputDataset, datasetId, userId);
        if (updatedDataset.isPresent()) {
            URI uri = info.getRequestUriBuilder().replacePath("api/dataset/{datasetId}").build(updatedDataset.get().getDataSetId());
            return Response.ok(uri).entity(updatedDataset.get()).build();
        }
        else {
            return Response.noContent().build();
        }
    }

    @GET
    @Produces("application/json")
    @PermitAll
    public Response describeDataSets(@Auth AuthUser authUser) {
        User user = userService.findUserByEmail(authUser.getName());
        Collection<DataSetDTO> dataSetList = datasetService.describeDatasets(user.getDacUserId());
        return Response.ok(dataSetList, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/{datasetId}")
    @Produces("application/json")
    @PermitAll
    public Response describeDataSet( @PathParam("datasetId") Integer datasetId){
        try {
            DataSetDTO datasetDTO = datasetService.getDatasetDTO(datasetId);
            return Response.ok(datasetDTO, MediaType.APPLICATION_JSON).build();
        } catch (Exception e){
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
            DataSet datasetWithName = datasetService.getDatasetByName(name);
            return Response.ok().entity(datasetWithName.getDataSetId()).build();
        } catch (Exception e) {
            throw new NotFoundException("Could not find the dataset with name: " + name);
        }
    }

    @GET
    @Path("/sample")
    @PermitAll
    public Response getDataSetSample() {
        String msg = "GETting Data Set Sample";
        logger().debug(msg);
        String fileName = "DataSetSample.tsv";
        InputStream inputStream = null;
        try {
            inputStream = Resources.getResource(fileName).openStream();
        } catch (IOException e) {
            Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            logger().error("Error when GETting dataset sample. Cause: " + e);
        }
        return Response.ok(inputStream).header("Content-Disposition", "attachment; filename=" + fileName).build();
    }

    @POST
    @Path("/download")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response downloadDataSets(List<Integer> idList) {
        String msg = "GETing DataSets to download";
        logger().debug(msg);

        JSONObject json = new JSONObject();

        Collection<Dictionary> headers  =  datasetService.describeDictionaryByReceiveOrder();

        StringBuilder sb = new StringBuilder();
        String TSV_DELIMITER = "\t";
        for(Dictionary header : headers) {
            if (sb.length() > 0)
                sb.append(TSV_DELIMITER);
            sb.append(header.getKey());
        }
        sb.append(END_OF_LINE);

        if (CollectionUtils.isEmpty(idList)) {
            json.put("datasets", sb.toString());
            return Response.ok(json.toString(), MediaType.APPLICATION_JSON).build();
        }

        Collection<DataSetDTO> rows = datasetService.describeDataSetsByReceiveOrder(idList);

        for (DataSetDTO row : rows) {
            StringBuilder sbr = new StringBuilder();
            DataSetPropertyDTO property = new DataSetPropertyDTO("Consent ID",row.getConsentId());
            List<DataSetPropertyDTO> props = row.getProperties();
            props.add(property);
            for (DataSetPropertyDTO prop : props) {
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

    }


    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{datasetId}")
    @RolesAllowed(ADMIN)
    public Response delete(@Auth AuthUser authUser, @PathParam("datasetId") Integer dataSetId, @Context UriInfo info) {
        try {
            User user = userService.findUserByEmail(authUser.getName());
            datasetService.deleteDataset(dataSetId, user.getDacUserId());
            return Response.ok().build();
        } catch (Exception e) {
            return Response.serverError().entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/disable/{datasetObjectId}/{active}")
    @RolesAllowed(ADMIN)
    public Response disableDataSet(@PathParam("datasetObjectId") Integer dataSetId, @PathParam("active") Boolean active, @Context UriInfo info) {
        datasetService.disableDataset(dataSetId, active);
        return Response.ok().build();
    }

    @GET
    @Path("/dictionary")
    @Produces("application/json")
    @PermitAll
    public Collection<Dictionary> describeDictionary(){
        return datasetService.describeDictionaryByDisplayOrder();
    }

    @GET
    @Path("/autocomplete/{partial}")
    @Produces("application/json")
    @PermitAll
    public Response datasetAutocomplete(@Auth AuthUser authUser, @PathParam("partial") String partial){
        User dacUser = userService.findUserByEmail(authUser.getName());
        Integer dacUserId = dacUser.getDacUserId();
        List<Map<String, String>> datasets = datasetService.autoCompleteDatasets(partial, dacUserId);

        return Response.ok(datasets, MediaType.APPLICATION_JSON).build();
    }

    @PUT
    @Produces("application/json")
    @RolesAllowed(ADMIN)
    public Response updateNeedsReviewDataSets(@QueryParam("dataSetId") Integer dataSetId, @QueryParam("needsApproval") Boolean needsApproval){
        try{
            DataSet dataSet = datasetService.updateNeedsReviewDataSets(dataSetId, needsApproval);
            return Response.ok().entity(dataSet).build();
        }catch (Exception e){
            return createExceptionResponse(e);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @PermitAll
    @Path("/{datasetId}/approved/users")
    public Response downloadDatasetApprovedUsers(@PathParam("datasetId") Integer datasetId) {
        try {
            return Response.ok(dataAccessRequestAPI.createDataSetApprovedUsersDocument(datasetId))
                    .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename =" + "DatasetApprovedUsers.tsv")
                    .build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @Override
    protected Logger logger() {
        return LoggerFactory.getLogger(this.getClass());
    }

}
