package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.security.PermitAll;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Summary;
import org.broadinstitute.consent.http.models.SummaryDetail;
import org.broadinstitute.consent.http.service.SummaryService;

@Path("api/consent/cases")
public class ConsentCasesResource extends Resource {

    private final SummaryService summaryService;

    @Inject
    public ConsentCasesResource(SummaryService summaryService) {
        this.summaryService = summaryService;
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
