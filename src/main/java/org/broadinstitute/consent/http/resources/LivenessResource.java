package org.broadinstitute.consent.http.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@Path("liveness")
public class LivenessResource {

    @GET
    @Produces("text/plain")
    public Response healthCheck() {
        return Response.ok("Healthy!").build();
    }
}
