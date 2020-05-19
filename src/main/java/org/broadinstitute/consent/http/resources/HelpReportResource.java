package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.models.HelpReport;
import org.broadinstitute.consent.http.service.AbstractHelpReportAPI;
import org.broadinstitute.consent.http.service.EmailNotifierService;
import org.broadinstitute.consent.http.service.HelpReportAPI;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

@Path("{api : (api/)?}report")
public class HelpReportResource extends Resource {

    private final HelpReportAPI helpReportAPI;
    private final EmailNotifierService emailNotifierService;

    @Inject
    public HelpReportResource(EmailNotifierService emailNotifierService) {
        this.helpReportAPI = AbstractHelpReportAPI.getInstance();
        this.emailNotifierService = emailNotifierService;
    }

    @POST
    @Consumes("application/json")
    @PermitAll
    public Response createdHelpReport(@Context UriInfo info, HelpReport helpReport) {
        try {
            helpReport = helpReportAPI.create(helpReport);
            emailNotifierService.sendNewRequestHelpMessage(helpReport);
            URI uri = info.getRequestUriBuilder().path("{id}").build(helpReport.getReportId());
            return Response.created(uri).entity(helpReport).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @RolesAllowed(ADMIN)
    public Response delete(@PathParam("id") Integer id) {
        helpReportAPI.deleteHelpReportById(id);
        return Response.ok().entity("Report was deleted").build();
    }

    @GET
    @Path("/{id}")
    @Produces("application/json")
    @PermitAll
    public Response describe(@PathParam("id") Integer id) {
        try{
            return Response.ok().entity(helpReportAPI.findHelpReportById(id)).build();
        }catch (Exception e){
            return createExceptionResponse(e);
        }

    }

    @GET
    @Produces("application/json")
    @Path("/user/{userId}")
    @PermitAll
    public List<HelpReport> describeAllReportsByUser(@PathParam("userId") Integer id) {
        return helpReportAPI.findHelpReportsByUserId(id);
    }

}
