package org.genomebridge.consent.http.resources;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.genomebridge.consent.http.service.AbstractDataSetAPI;
import org.genomebridge.consent.http.service.DataSetAPI;
import org.genomebridge.consent.http.service.UnknownIdentifierException;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.InputStream;

@Path("/dataset")
public class DataSetResource extends Resource {


    private DataSetAPI api;

    public DataSetResource() {
        this.api = AbstractDataSetAPI.getInstance();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createDataSet(
            @FormDataParam("data") InputStream uploadedDataSet,
            @FormDataParam("data") FormDataBodyPart part) {
            String msg = String.format("POSTing Data Set");
            logger().debug(msg);

        if( part.getMediaType().toString().equals(MediaType.TEXT_PLAIN)) {
              try {
                  File targetFile = new File(part.getContentDisposition().getFileName());
                  FileUtils.copyInputStreamToFile(uploadedDataSet, targetFile);
                  return Response.ok( api.create(targetFile)).build();
            } catch (Exception e) {
                return Response.status(Response.Status.BAD_REQUEST).entity(e).build();
          }
         }
           return Response.status(Response.Status.BAD_REQUEST).build();
       }

    @GET
    @Path("/{id}")
    public Response getDataSet(@PathParam("id") String dataSetId) {
        String msg = String.format("GETing Data Set with id '%s'", dataSetId);
        logger().debug(msg);
        try {
            return Response.ok(api.retrieve(dataSetId)).build();
        } catch (UnknownIdentifierException e) {
            throw new NotFoundException(String.format("Could not find dataSet with id %s", dataSetId));
        }

    }

    @Override
    protected Logger logger() {
        return Logger.getLogger("DataSetResource");
    }

}
