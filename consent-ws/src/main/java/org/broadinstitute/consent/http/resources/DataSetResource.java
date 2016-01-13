package org.broadinstitute.consent.http.resources;

import com.google.common.io.Resources;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.dto.*;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.AbstractDataSetAPI;
import org.broadinstitute.consent.http.service.DataSetAPI;
import org.broadinstitute.consent.http.service.ParseResult;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Path("{api : (api/)?}dataset")
public class DataSetResource extends Resource {

    private final String END_OF_LINE = System.lineSeparator();
    private final String TSV_DELIMITER = "\t";
    private final DataSetAPI api;

    public DataSetResource() {
        this.api = AbstractDataSetAPI.getInstance();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("application/json")
    public Response createDataSet(
            @FormDataParam("data") InputStream uploadedDataSet,
            @FormDataParam("data") FormDataBodyPart part,
            @DefaultValue("false") @QueryParam("overwrite") boolean overwrite) throws IOException {

        logger().debug("POSTing Data Set");
        List<DataSet> dataSets;
        List<String> errors = new ArrayList<>();
        if (part.getMediaType().getType().equals("text") && 
                (part.getMediaType().getSubtype().equals("tab-separated-values") 
                || part.getMediaType().getSubtype().equals("plain") )) {
            File inputFile = null;
            try {
                inputFile = new File(UUID.randomUUID().toString());
                FileUtils.copyInputStreamToFile(uploadedDataSet, inputFile);
                ParseResult result;
                if(overwrite) {
                    result = api.overwrite(inputFile);
                }else{
                    result = api.create(inputFile);
                }
                dataSets = result.getDatasets();
                errors = result.getErrors();

                if (CollectionUtils.isNotEmpty(errors)) {
                    // errors should be download as a file, not implemented yet
                    return Response.status(Response.Status.BAD_REQUEST).entity(errors).build();
                } else {
                    // datasets should be download as a file ?, if so, not implemented yet
                    return Response.ok(dataSets, MediaType.APPLICATION_JSON).build();
                }
            } catch (Exception e) {
                logger().fatal("POSTing Data Set", e);
                errors.add("A problem has occurred while uploading datasets - Contact Support");
            } finally {
                if (inputFile != null) {
                    inputFile.delete();
                }
            }
        }

        errors.add("The file type is not the expected one. Please download the Dataset Spreadsheet Model from the 'Add Datasets' window.");
        return Response.status(Response.Status.BAD_REQUEST).entity(errors).build();
    }

    @GET
    @Produces("application/json")
    public Response describeDataSets(@Context HttpServletRequest request , @QueryParam("dacUserId") Integer dacUserId){
        if (StringUtils.isEmpty(request.getParameter("dacUserId"))) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }else{
            Collection<DataSetDTO> dataSetList = api.describeDataSets(dacUserId);
            return Response.ok(dataSetList, MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/{datasetId}")
    @Produces("application/json")
    public Response describeDataSet( @PathParam("datasetId") String datasetId){
        try {
            return Response.ok(api.getDataSetDTO(datasetId), MediaType.APPLICATION_JSON).build();
        } catch (NotFoundException e){
            return Response.status(Response.Status.NOT_FOUND).entity(new Error(e.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build();
        }
    }


    @GET
    @Path("/sample")
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
    public Response downloadDataSets(List<String> idList) {
        String msg = "GETing DataSets to download";
        logger().debug(msg);

        JSONObject json = new JSONObject();

        Collection<Dictionary> headers  =  api.describeDictionary();

        StringBuilder sb = new StringBuilder();
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

        Collection<DataSetDTO> rows = api.describeDataSets(idList);

        for (DataSetDTO row : rows) {
            StringBuilder sbr = new StringBuilder();
            List<DataSetPropertyDTO> props = row.getProperties();
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
    @Path("/{datasetObjectId}")
    public Response delete(@PathParam("datasetObjectId") String datasetObjectId, @Context UriInfo info) {
        api.deleteDataset(datasetObjectId);
        return Response.ok().build();
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/disable/{datasetObjectId}/{active}")
    public Response disableDataSet(@PathParam("datasetObjectId") String datasetObjectId, @PathParam("active") Boolean active, @Context UriInfo info) {
        api.disableDataset(datasetObjectId, active);
        return Response.ok().build();
    }

    @GET
    @Path("/dictionary")
    @Produces("application/json")
    public Collection<Dictionary> describeDictionary(){
        return api.describeDictionary();
    }

    @GET
    @Path("/autocomplete/{partial}")
    @Produces("application/json")
    public Response datasetAutocomplete(@PathParam("partial") String partial){
        List<Map<String, String>> j = api.autoCompleteDataSets(partial);
        return Response.ok(j, MediaType.APPLICATION_JSON).build();
    }

    @PUT
    @Produces("application/json")
    public Response updateNeedsReviewDataSets(@QueryParam("dataSetId") String dataSetId, @QueryParam("needsApproval") Boolean needsApproval){
        try{
            DataSet dataSet = api.updateNeedsReviewDataSets(dataSetId, needsApproval);
            return Response.ok().entity(dataSet).build();
        }catch (NotFoundException e){
            return Response.status(Response.Status.NOT_FOUND).entity(new Error(e.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build();
        }
    }

    @Override
    protected Logger logger() {
        return Logger.getLogger("DataSetResource");
    }

}
