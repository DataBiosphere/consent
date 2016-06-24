package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.PendingCase;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.*;

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

    private final PendingCaseAPI api;
    private final SummaryAPI summaryApi;
    private final ElectionAPI electionApi;

    public DataRequestCasesResource() {
        this.api = AbstractPendingCaseAPI.getInstance();
        this.summaryApi = AbstractSummaryAPI.getInstance();
        this.electionApi = AbstractElectionAPI.getInstance();
    }

    @GET
    @Path("/pending/{dacUserId}")
    @RolesAllowed({"CHAIRPERSON", "MEMBER"})
    public List<PendingCase> getDataRequestPendingCases(@PathParam("dacUserId") Integer dacUserId) {
        return api.describeDataRequestPendingCases(dacUserId);
    }

    @GET
    @Path("/pending/dataOwner/{dataOwnerId}")
    @RolesAllowed({"CHAIRPERSON", "DATAOWNER"})
    public Response getDataOwnerPendingCases(@PathParam("dataOwnerId") Integer dataOwnerId) {
        try{
            return Response.ok(api.describeDataOwnerPendingCases(dataOwnerId)).build();
        }catch(Exception e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }

    }

    @GET
    @Path("/summary/{type}")
    @PermitAll
    public Response getDataRequestSummaryCases(@PathParam("type") String type) {
        return Response.ok(summaryApi.describeDataRequestSummaryCases(type))
                .build();
    }


    @GET
    @Path("/matchsummary")
    @PermitAll
    public Response getMatchSummaryCases() {
        return Response.ok(summaryApi.describeMatchSummaryCases())
                .build();
    }


    @GET
    @Path("/closed")
    @Produces("application/json")
    @RolesAllowed({"CHAIRPERSON", "MEMBER","ALUMNI","ADMIN"})
    public List<Election> describeClosedElections() {
        return electionApi.describeClosedElectionsByType(ElectionType.DATA_ACCESS.getValue());
    }

}