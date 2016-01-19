package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.models.HelpReport;
import org.broadinstitute.consent.http.models.dto.DefaultErrorMessage;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.AbstractHelpReportAPI;
import org.broadinstitute.consent.http.service.HelpReportAPI;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

@Path("{api : (api/)?}report")
public class HelpReportResource extends Resource {

    private final HelpReportAPI helpReportAPI;

    public HelpReportResource(){
        this.helpReportAPI = AbstractHelpReportAPI.getInstance();
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response createdHelpReport(@Context UriInfo info, HelpReport helpReport) {
        try {
            helpReport = helpReportAPI.create(helpReport);
            URI uri = info.getRequestUriBuilder().path("{id}").build(helpReport.getReportId());
            return Response.created(uri).entity(helpReport).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e).build();
        } catch (Exception e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new Error(DefaultErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
   }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Response delete(@PathParam("id") Integer id) {
        try{
            helpReportAPI.deleteHelpReportById(id);
        }catch (NotFoundException e){
            return Response.status(Response.Status.NOT_FOUND).entity(new Error(DefaultErrorMessage.NOT_FOUND.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build();
        }
        return Response.status(Response.Status.OK).build();
    }

    @GET
    @Path("/{id}")
    @Produces("application/json")
    public Response describe(@PathParam("id") Integer id) {
        try{
            return Response.ok().entity(helpReportAPI.findHelpReportById(id)).build();
        }catch (NotFoundException e){
            return Response.status(Response.Status.NOT_FOUND).entity(new Error(DefaultErrorMessage.NOT_FOUND.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build();
        }
    }

    @GET
    @Produces("application/json")
    @Path("/user/{userId}")
    public List<HelpReport> describeAllReportsByUser(@PathParam("userId") Integer id) {
        return helpReportAPI.findHelpReportsByUserId(id);
    }

}
