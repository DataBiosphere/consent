package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.PendingCase;
import org.broadinstitute.consent.http.models.Summary;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.PendingCaseService;
import org.broadinstitute.consent.http.service.SummaryService;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("api/dataRequest/cases")
public class DataRequestCasesResource extends Resource {

    private final PendingCaseService pendingCaseService;
    private final SummaryService summaryService;

    @Inject
    public DataRequestCasesResource(PendingCaseService pendingCaseService, SummaryService summaryService) {
        this.pendingCaseService = pendingCaseService;
        this.summaryService = summaryService;
    }

    @Deprecated
    @GET
    @Path("/pending")
    @RolesAllowed({CHAIRPERSON, MEMBER})
    public Response getDataRequestPendingCasesByAuthUser(@Auth AuthUser authUser) {
        List<PendingCase> pendingCases = pendingCaseService.describeDataRequestPendingCases(authUser);
        return Response.ok().entity(pendingCases).build();
    }

    @GET
    @Path("/summary/{type}")
    @PermitAll
    public Response getDataRequestSummaryCases(@PathParam("type") String type, @Auth AuthUser authUser) {
        Summary summary = summaryService.describeDataRequestSummaryCases(type);
        return Response.ok().entity(summary).build();
    }


    @GET
    @Path("/matchsummary")
    @PermitAll
    public Response getMatchSummaryCases(@Auth AuthUser authUser) {
        List<Summary> summaries = summaryService.describeMatchSummaryCases();
        return Response.ok().entity(summaries).build();
    }


}
