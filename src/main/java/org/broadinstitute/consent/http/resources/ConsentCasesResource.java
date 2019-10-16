package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.PendingCase;
import org.broadinstitute.consent.http.models.Summary;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.AbstractSummaryAPI;
import org.broadinstitute.consent.http.service.ConsentService;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.broadinstitute.consent.http.service.SummaryAPI;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.File;
import java.util.List;

@Path("api/consent/cases")
public class ConsentCasesResource extends Resource {

    private final ConsentService consentService;
    private final SummaryAPI summaryApi;
    private final ElectionAPI electionApi;

    @Inject
    public ConsentCasesResource(ConsentService consentService) {
        this.consentService = consentService;
        this.summaryApi = AbstractSummaryAPI.getInstance();
        this.electionApi = AbstractElectionAPI.getInstance();
    }

    @GET
    @Path("/pending/{dacUserId}")
    @RolesAllowed({MEMBER, CHAIRPERSON})
    public Response getConsentPendingCases(@PathParam("dacUserId") Integer dacUserId, @Auth AuthUser authUser) {
        List<PendingCase> pendingCases = consentService.describeConsentPendingCases(authUser);
        return Response.ok().entity(pendingCases).build();
    }

    @GET
    @Path("/summary")
    @PermitAll
    public Response getConsentSummaryCases(@Auth AuthUser authUser) {
        Summary summary = summaryApi.describeConsentSummaryCases();
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
            fileToSend = summaryApi.describeConsentSummaryDetail();
        } else if (fileType.equals(ElectionType.DATA_ACCESS.getValue())) {
            fileToSend = summaryApi.describeDataAccessRequestSummaryDetail();
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
        List<Election> elections = electionApi.describeClosedElectionsByType(ElectionType.TRANSLATE_DUL.getValue());
        return Response.ok().entity(elections).build();
    }

}
