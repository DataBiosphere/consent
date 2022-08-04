package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.PendingCase;
import org.broadinstitute.consent.http.models.Summary;
import org.broadinstitute.consent.http.models.SummaryDetail;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.PendingCaseService;
import org.broadinstitute.consent.http.service.SummaryService;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Path("api/consent/cases")
public class ConsentCasesResource extends Resource {

    private final PendingCaseService pendingCaseService;
    private final SummaryService summaryService;

    @Inject
    public ConsentCasesResource(PendingCaseService pendingCaseService, SummaryService summaryService) {
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
    public Response getConsentSummaryDetailFile(@QueryParam("type") String type, @Auth AuthUser authUser) {
        try {
            if (Objects.isNull(type)) {
                type = ElectionType.DATA_ACCESS.getValue();
            }
            List<? extends SummaryDetail> details = new ArrayList<>();
            if (type.equals(ElectionType.TRANSLATE_DUL.getValue())) {
                details = summaryService.describeConsentSummaryDetail();
            } else if (type.equals(ElectionType.DATA_ACCESS.getValue())) {
                details = summaryService.listDataAccessRequestSummaryDetails();
            }
            if (!details.isEmpty()) {
                StringBuilder detailsBuilder = new StringBuilder();
                detailsBuilder.append(details.get(0).headers()).append(System.lineSeparator());
                details.forEach(d -> detailsBuilder.append(d.toString()).append(System.lineSeparator()));
                return Response.ok(detailsBuilder.toString()).build();
            }
            return Response.ok().build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }


}
