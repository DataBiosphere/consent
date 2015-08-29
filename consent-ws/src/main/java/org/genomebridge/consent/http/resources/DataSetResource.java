package org.genomebridge.consent.http.resources;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.genomebridge.consent.http.models.DataSet;
import org.genomebridge.consent.http.service.AbstractDataSetAPI;
import org.genomebridge.consent.http.service.DataSetAPI;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@Path("/dataset")
public class DataSetResource extends Resource {

    private DataSetAPI api;

    public DataSetResource() {
        this.api = AbstractDataSetAPI.getInstance();
    }

    @POST
    @Path("/{overwrite}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createDataSet(
            @FormDataParam("data") InputStream uploadedDataSet,
            @FormDataParam("data") FormDataBodyPart part,
            @DefaultValue("false") @PathParam("overwrite") boolean overwrite) {
        
        logger().debug("POSTing Data Set");
        List<DataSet> dataSets = new ArrayList<DataSet>();
        List<String> errors = new ArrayList<String>();
        if (part.getMediaType().toString().equals(MediaType.TEXT_PLAIN)) {
            try {
                File inputFile = new File(part.getContentDisposition().getFileName());
                FileUtils.copyInputStreamToFile(uploadedDataSet, inputFile);
                Map<String, Object> result;
                if(overwrite) {
                    result = api.overwrite(inputFile);
                }else{
                    result = api.create(inputFile);
                }
                dataSets = (List<DataSet>) result.get("datasets");
                errors = (List<String>) result.get("validationsErrors");
                if (CollectionUtils.isNotEmpty(errors)) {
                    // errors should be download as a file, not implemented yet
                    return Response.status(Response.Status.BAD_REQUEST).entity(errors).build();
                } else {
                    // datasets should be download as a file ?, if so, not implemented yet
                    return Response.ok(dataSets, MediaType.APPLICATION_JSON).build();
                }
            } catch (Exception e) {
                logger().fatal("POSTing Data Set", e);
                errors.add("A problem has ocurred while uploading datasets - Contact Support");
            }
        }
        return Response.status(Response.Status.BAD_REQUEST).entity(errors).build();
    }

    @GET
    @Produces("application/json")
    public Collection<DataSet> describeDataSets(){
        return api.describeDataSets();
    }

    @GET
    @Path("/sample")
    public Response getDataSetSample() {
        String msg = "GETing Data Set Sample";
        logger().debug(msg);

        String fileName = "DataSetSample.tsv";

        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);
        File targetFile = new File(fileName);

        try {
            FileUtils.copyInputStreamToFile(inputStream, targetFile);

        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(DataSetResource.class.getName()).log(Level.SEVERE, null, ex);
        }

        return Response.ok(targetFile)
            .header("Content-Disposition", "attachment; filename=" + targetFile.getName())
                .build();
    }

    @POST
    @Path("/download")
    public Response downloadDataSets(List<String> idList) {
        String msg = "GETing DataSets to download";
        logger().debug(msg);

        String fileName = "DownloadedDataSet.tsv";

        File targetFile = new File(fileName);

//        try {
//            FileUtils.copyInputStreamToFile(inputStream, targetFile);
//
//        } catch (IOException ex) {
//            java.util.logging.Logger.getLogger(DataSetResource.class.getName()).log(Level.SEVERE, null, ex);
//        }

        return Response.ok(targetFile)
            .header("Content-Disposition", "attachment; filename=" + targetFile.getName())
                .build();
    }

    @Override
    protected Logger logger() {
        return Logger.getLogger("DataSetResource");
    }

}
