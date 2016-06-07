package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.ElectionAPI;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
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
    @RolesAllowed({"ADMIN", "DATAOWNER","CHAIRPERSON","MEMBER"})
    public Response updateElection(Election rec, @PathParam("id") Integer id) {
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
    @PermitAll
    public Response describeElectionById(@PathParam("id") Integer id) {
        try {
            return Response.ok().entity(api.describeElectionById(id)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).entity(new Error(e.getMessage(), Status.BAD_REQUEST.getStatusCode())).build();
        }
    }

    @GET
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/checkdataset")
    @PermitAll
    public Response isDataSetElectionOpen(@Context UriInfo info) {
        try {
            return Response.ok().entity("{ \"open\" : " + api.isDataSetElectionOpen() + " }").build();
        } catch (Exception e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Error(e.getMessage(), Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }


}