package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.util.JsonSchemaUtil;

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
