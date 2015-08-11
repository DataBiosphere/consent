package org.genomebridge.consent.http.resources;

import java.io.File;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.genomebridge.consent.http.enumeration.ElectionType;
import org.genomebridge.consent.http.models.Election;
import org.genomebridge.consent.http.service.*;

@Path("/consent/cases")
public class ConsentCasesResource extends Resource {

    private PendingCaseAPI api;
    private SummaryAPI summaryApi;
    private ElectionAPI electionApi;

    public ConsentCasesResource() {
        this.api = AbstractPendingCaseAPI.getInstance();
        this.summaryApi = AbstractSummaryAPI.getInstance();
        this.electionApi = AbstractElectionAPI.getInstance();
    }

    @GET
    @Path("/pending/{dacUserId}")
    public Response getConsentPendingCases(@PathParam("dacUserId") Integer dacUserId) {
        return Response.ok(api.describeConsentPendingCases(dacUserId))
                .build();
    }

    @GET
    @Path("/summary")
    public Response getConsentSummaryCases() {
        return Response.ok(summaryApi.describeConsentSummaryCases())
                .build();
    }

    @GET
    @Path("/summary/file")
    @Produces("text/plain")
    public Response getConsentSummaryDetailFile() {
        File fileToSend = summaryApi.describeConsentSummaryDetail();
        ResponseBuilder response = Response.ok(fileToSend);
        response.header("Content-Disposition", "attachment; filename=\"summary.txt\"");
        return response.build();
    }

    @GET
    @Path("/closed")
    @Produces("application/json")
    public List<Election> describeClosedElections() {
        return electionApi.describeClosedElectionsByType("2");
    }

}
