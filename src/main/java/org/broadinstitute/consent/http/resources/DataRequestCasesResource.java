package org.broadinstitute.consent.http.resources;

import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.PendingCase;
import org.broadinstitute.consent.http.models.Summary;
import org.broadinstitute.consent.http.models.dto.DataOwnerCase;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.AbstractPendingCaseAPI;
import org.broadinstitute.consent.http.service.AbstractSummaryAPI;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.PendingCaseAPI;
import org.broadinstitute.consent.http.service.SummaryAPI;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("{api : (api/)?}dataRequest/cases")
public class DataRequestCasesResource extends Resource {

    private final ElectionService electionService;
    private final PendingCaseAPI api;
    private final SummaryAPI summaryApi;

    public DataRequestCasesResource(ElectionService electionService) {
        this.electionService = electionService;
        this.api = AbstractPendingCaseAPI.getInstance();
        this.summaryApi = AbstractSummaryAPI.getInstance();
    }

    @GET
    @Path("/pending/{dacUserId}")
    @RolesAllowed({CHAIRPERSON, MEMBER})
    public Response getDataRequestPendingCases(@PathParam("dacUserId") Integer dacUserId, @Auth AuthUser authUser) {
        List<PendingCase> pendingCases = api.describeDataRequestPendingCases(dacUserId);
        return Response.ok().entity(pendingCases).build();
    }

    @GET
    @Path("/pending/dataOwner/{dataOwnerId}")
    @RolesAllowed({CHAIRPERSON, DATAOWNER})
    public Response getDataOwnerPendingCases(@PathParam("dataOwnerId") Integer dataOwnerId, @Auth AuthUser authUser) {
        try {
            List<DataOwnerCase> ownerCases = api.describeDataOwnerPendingCases(dataOwnerId);
            return Response.ok().entity(ownerCases).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).
                    entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).
                    build();
        }
    }

    @GET
    @Path("/summary/{type}")
    @PermitAll
    public Response getDataRequestSummaryCases(@PathParam("type") String type, @Auth AuthUser authUser) {
        Summary summary = summaryApi.describeDataRequestSummaryCases(type);
        return Response.ok().entity(summary).build();
    }


    @GET
    @Path("/matchsummary")
    @PermitAll
    public Response getMatchSummaryCases(@Auth AuthUser authUser) {
        List<Summary> summaries = summaryApi.describeMatchSummaryCases();
        return Response.ok().entity(summaries).build();
    }


    @GET
    @Path("/closed")
    @Produces("application/json")
    @RolesAllowed({CHAIRPERSON, MEMBER, ALUMNI, ADMIN})
    public Response describeClosedElections(@Auth AuthUser authUser) {
        List<Election> elections = electionService.describeClosedElectionsByType(ElectionType.DATA_ACCESS.getValue(), authUser);
        return Response.ok().entity(elections).build();
    }

}