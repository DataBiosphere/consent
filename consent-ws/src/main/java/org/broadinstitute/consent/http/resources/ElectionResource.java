package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.ElectionAPI;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

@Path("{api : (api/)?}election/")
public class ElectionResource extends Resource {

    private final ElectionAPI api;

    public ElectionResource() {
        this.api = AbstractElectionAPI.getInstance();
    }


    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/{id}")
    public Response updateElection(@Context UriInfo info, Election rec, @PathParam("id") Integer id) {
        try {
            return Response.ok().entity(api.updateElectionById(rec, id)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).entity(new Error(e.getMessage(), Status.BAD_REQUEST.getStatusCode())).build();
        }
    }

    @GET
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/{id}")
    public Response describeElectionById(@Context UriInfo info, @PathParam("id") Integer id) {
        try {
            return Response.ok().entity(api.describeElectionById(id)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).entity(new Error(e.getMessage(), Status.BAD_REQUEST.getStatusCode())).build();
        }
    }


}
