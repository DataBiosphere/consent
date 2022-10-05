package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.util.JsonSchemaUtil;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/schemas")
public class SchemaResource extends Resource {

    private final JsonSchemaUtil jsonSchemaUtil;

    @Inject
    public SchemaResource() {
        this.jsonSchemaUtil = new JsonSchemaUtil();
    }
    @GET
    @Path("/dataset-registration/v1")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDatasetRegistrationSchemaV1() {
        String content = jsonSchemaUtil.getDatasetRegistrationSchemaV1();
        return Response.ok().entity(content).type(MediaType.APPLICATION_JSON).build();
    }

}
