package org.genomebridge.consent.http.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.genomebridge.consent.http.models.PendingCase;
import org.genomebridge.consent.http.service.AbstractPendingCaseAPI;
import org.genomebridge.consent.http.service.AbstractSummaryAPI;
import org.genomebridge.consent.http.service.PendingCaseAPI;
import org.genomebridge.consent.http.service.SummaryAPI;

@Path("/dataRequest/cases")
public class DataRequestCasesResource extends Resource {

    private PendingCaseAPI api;
    private SummaryAPI summaryApi;

    public DataRequestCasesResource() {
        this.api = AbstractPendingCaseAPI.getInstance();
        this.summaryApi = AbstractSummaryAPI.getInstance();
    }

    @GET
    @Path("/pending/{dacUserId}")
    public List<PendingCase> getDataRequestPendingCases(@PathParam("dacUserId") Integer dacUserId) {
        return api.describeDataRequestPendingCases(dacUserId);
    }

    @GET
    @Path("/summary")
    public Response getConsentSummaryCases() {
        return Response.ok(summaryApi.describeDataRequestSummaryCases())
                .build();
    }

}
