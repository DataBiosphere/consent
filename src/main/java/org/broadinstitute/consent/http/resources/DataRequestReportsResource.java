package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.ResearcherService;

@Path("{api : (api/)?}dataRequest")
public class DataRequestReportsResource extends Resource {

    private final DataAccessRequestService darService;

    private final ResearcherService researcherService;

    private final UserService userService;

    @Inject
    public DataRequestReportsResource(DataAccessRequestService darService,
        ResearcherService researcherService, UserService userService) {
        this.darService = darService;
        this.researcherService = researcherService;
        this.userService = userService;
    }

    // TODO: Undocumented
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @PermitAll
    @Path("/approved")
    public Response downloadApprovedDARs() {
        try {
            return Response.ok(darService.createApprovedDARDocument())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename =" + "ApprovedDataAccessRequests.tsv")
                    .build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    // TODO: Undocumented
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @PermitAll
    @Path("/reviewed")
    public Response downloadReviewedDARs() {
        try {
            return Response.ok(darService.createReviewedDARDocument())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename =" + "ReviewedDataAccessRequests.tsv")
                    .build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

}
