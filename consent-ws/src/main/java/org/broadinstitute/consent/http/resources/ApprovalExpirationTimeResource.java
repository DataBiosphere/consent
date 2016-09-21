package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.models.ApprovalExpirationTime;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.AbstractApprovalExpirationTimeAPI;
import org.broadinstitute.consent.http.service.ApprovalExpirationTimeAPI;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@Path("{api : (api/)?}approvalExpirationTime")
public class ApprovalExpirationTimeResource extends Resource {

    private final ApprovalExpirationTimeAPI approvalExpirationTimeAPI;

    public ApprovalExpirationTimeResource(){
        this.approvalExpirationTimeAPI = AbstractApprovalExpirationTimeAPI.getInstance();
    }

    @POST
    @Consumes("application/json")
    @RolesAllowed("ADMIN")
    public Response createdApprovalExpirationTime(@Context UriInfo info, ApprovalExpirationTime approvalExpirationTime)  {
        URI uri;
        try {
            approvalExpirationTime = approvalExpirationTimeAPI.create(approvalExpirationTime);
            uri = info.getRequestUriBuilder().path("{id}").build(approvalExpirationTime.getId());
            return Response.created(uri).entity(approvalExpirationTime).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build();
        }
    }

    @GET
    @Produces("application/json")
    @RolesAllowed("ADMIN")
    public Response describeApprovalExpirationTime() {
        try{
            return Response.ok().entity(approvalExpirationTimeAPI.findApprovalExpirationTime()).build();
        }catch (Exception e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }

    }

    @GET
    @Path("/{id}")
    @Produces("application/json")
    @RolesAllowed("ADMIN")
    public Response describe(@PathParam("id") Integer id) {
        try{
            return Response.ok().entity(approvalExpirationTimeAPI.findApprovalExpirationTimeById(id)).build();
        }catch (NotFoundException e){
            return Response.status(Response.Status.NOT_FOUND).entity(new Error(e.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build();
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes("application/json")
    @Produces("application/json")
    @RolesAllowed("ADMIN")
    public Response update(@Context UriInfo info, ApprovalExpirationTime approvalExpirationTime, @PathParam("id") Integer id) {
        try {
            URI uri = info.getRequestUriBuilder().path("{id}").build(id);
            approvalExpirationTime = approvalExpirationTimeAPI.update(approvalExpirationTime, id);
            return Response.ok(uri).entity(approvalExpirationTime).build();
        } catch (Exception e) {
           return createExceptionResponse(e);
        }

    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Response delete(@PathParam("id") Integer id) {
        approvalExpirationTimeAPI.deleteApprovalExpirationTime(id);
        return Response.ok().entity("Approval expiration time was deleted").build();
    }


}
