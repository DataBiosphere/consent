package org.genomebridge.consent.http.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.genomebridge.consent.http.models.Election;
import org.genomebridge.consent.http.models.PendingCase;
import org.genomebridge.consent.http.service.*;

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
    public List<PendingCase> getDataRequestPendingCases(@PathParam("dacUserId") Integer dacUserId) {
        return api.describeDataRequestPendingCases(dacUserId);
    }

    @GET
    @Path("/summary/{type}")
    public Response getDataRequestSummaryCases(@PathParam("type") String type) {
        return Response.ok(summaryApi.describeDataRequestSummaryCases(type))
                .build();
    }


    @GET
    @Path("/matchsummary")
    public Response getMatchSummaryCases() {
        return Response.ok(summaryApi.describeMatchSummaryCases())
              .build();
    }


    @GET
    @Path("/closed")
    @Produces("application/json")
    public List<Election> describeClosedElections() {
        return electionApi.describeClosedElectionsByType("1");
    }

}