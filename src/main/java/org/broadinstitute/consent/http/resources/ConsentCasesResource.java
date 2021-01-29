package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import java.io.File;
import java.util.List;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.PendingCase;
import org.broadinstitute.consent.http.models.Summary;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.PendingCaseService;
import org.broadinstitute.consent.http.service.SummaryService;

@Path("{api : (api/)?}consent/cases")
public class ConsentCasesResource extends Resource {

    private final ElectionService electionService;
    private final PendingCaseService pendingCaseService;
    private final SummaryService summaryService;

    @Inject
    public ConsentCasesResource(ElectionService electionService, PendingCaseService pendingCaseService, SummaryService summaryService) {
        this.electionService = electionService;
        this.pendingCaseService = pendingCaseService;
        this.summaryService = summaryService;
    }

    @GET
    @Path("/pending/{dacUserId}")
    @RolesAllowed({MEMBER, CHAIRPERSON})
    public Response getConsentPendingCases(@PathParam("dacUserId") Integer dacUserId, @Auth AuthUser authUser) {
        List<PendingCase> pendingCases = pendingCaseService.describeConsentPendingCases(authUser);
        return Response.ok().entity(pendingCases).build();
    }

    @GET
    @Path("/summary")
    @PermitAll
    public Response getConsentSummaryCases(@Auth AuthUser authUser) {
        Summary summary = summaryService.describeConsentSummaryCases();
        return Response.ok().entity(summary).build();
    }

    @GET
    @Path("/summary/file")
    @Produces("text/plain")
    @PermitAll
    public Response getConsentSummaryDetailFile(@QueryParam("fileType") String fileType, @Auth AuthUser authUser) {
        ResponseBuilder response;
        File fileToSend = null;
        if (fileType.equals(ElectionType.TRANSLATE_DUL.getValue())) {
            fileToSend = summaryService.describeConsentSummaryDetail();
        } else if (fileType.equals(ElectionType.DATA_ACCESS.getValue())) {
            fileToSend = summaryService.describeDataAccessRequestSummaryDetail();
        }
        if ((fileToSend != null)) {
            response = Response.ok(fileToSend);
        } else {
            response = Response.ok();
        }
        return response.build();
    }

    @GET
    @Path("/closed")
    @Produces("application/json")
    @RolesAllowed({MEMBER, CHAIRPERSON, ALUMNI, ADMIN})
    public Response describeClosedElections(@Auth AuthUser authUser) {
        List<Election> elections = electionService.describeClosedElectionsByType(ElectionType.TRANSLATE_DUL.getValue(), authUser);
        return Response.ok().entity(elections).build();
    }

}
