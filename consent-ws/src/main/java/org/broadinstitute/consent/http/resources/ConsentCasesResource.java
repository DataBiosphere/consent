package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.service.*;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.File;
import java.util.List;

@Path("{api : (api/)?}consent/cases")
public class ConsentCasesResource extends Resource {

    private final PendingCaseAPI api;
    private final SummaryAPI summaryApi;
    private final ElectionAPI electionApi;

    public ConsentCasesResource() {
        this.api = AbstractPendingCaseAPI.getInstance();
        this.summaryApi = AbstractSummaryAPI.getInstance();
        this.electionApi = AbstractElectionAPI.getInstance();
    }

    @GET
    @Path("/pending/{dacUserId}")
    @RolesAllowed({"MEMBER", "CHAIRPERSON"})
    public Response getConsentPendingCases(@PathParam("dacUserId") Integer dacUserId) {
        return Response.ok(api.describeConsentPendingCases(dacUserId))
                .build();
    }

    @GET
    @Path("/summary")
    @PermitAll
    public Response getConsentSummaryCases() {
        return Response.ok(summaryApi.describeConsentSummaryCases())
                .build();
    }

    @GET
    @Path("/summary/file")
    @Produces("text/plain")
    @PermitAll
    public Response getConsentSummaryDetailFile(@QueryParam("fileType") String fileType) {
        ResponseBuilder response;
        File fileToSend = null;
        if (fileType.equals(ElectionType.TRANSLATE_DUL.getValue())) {
            fileToSend = summaryApi.describeConsentSummaryDetail();
        }else if (fileType.equals(ElectionType.DATA_ACCESS.getValue())){
            fileToSend = summaryApi.describeDataAccessRequestSummaryDetail();
        }
        if ((fileToSend != null)) {
            response = Response.ok(fileToSend);
        } else response = Response.ok();
        return response.build();
    }

    @GET
    @Path("/closed")
    @Produces("application/json")
    @RolesAllowed({"MEMBER", "CHAIRPERSON", "ALUMNI", "ADMIN"})
    public List<Election> describeClosedElections() {
        return electionApi.describeClosedElectionsByType(ElectionType.TRANSLATE_DUL.getValue());
    }

}