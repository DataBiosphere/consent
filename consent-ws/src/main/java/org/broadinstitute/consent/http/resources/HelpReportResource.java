package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.models.HelpReport;
import org.broadinstitute.consent.http.service.*;

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
    public Response createdHelpReport(@Context UriInfo info, HelpReport helpReport) {
        try {
            helpReport = helpReportAPI.create(helpReport);
            URI uri = info.getRequestUriBuilder().path("{id}").build(helpReport.getReportId());
            return Response.created(uri).entity(helpReport).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e).build();
        }
   }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Response delete(@PathParam("id") Integer id) {
        helpReportAPI.deleteHelpReportById(id);
        return Response.ok().entity("Report was deleted").build();
    }

    @GET
    @Path("/{id}")
    @Produces("application/json")
    public Response describe(@PathParam("id") Integer id) {
        try{
            return Response.ok().entity(helpReportAPI.findHelpReportById(id)).build();
        }catch (NotFoundException e){
            return Response.status(Response.Status.NOT_FOUND).entity(e).build();
        }

    }

    @GET
    @Produces("application/json")
    @Path("/user/{userId}")
    public List<HelpReport> describeAllReportsByUser(@PathParam("userId") Integer id) {
        return helpReportAPI.findHelpReportsByUserId(id);
    }

}
