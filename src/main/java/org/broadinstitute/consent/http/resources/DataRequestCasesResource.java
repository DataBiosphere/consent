package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import java.util.List;
import javax.annotation.security.PermitAll;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Summary;
import org.broadinstitute.consent.http.service.SummaryService;

@Path("api/dataRequest/cases")
public class DataRequestCasesResource extends Resource {

  private final SummaryService summaryService;

  @Inject
  public DataRequestCasesResource(SummaryService summaryService) {
    this.summaryService = summaryService;
  }

  @GET
  @Path("/summary/{type}")
  @PermitAll
  public Response getDataRequestSummaryCases(@PathParam("type") String type,
      @Auth AuthUser authUser) {
    Summary summary = summaryService.describeDataRequestSummaryCases(type);
    return Response.ok().entity(summary).build();
  }


  @GET
  @Path("/matchsummary")
  @PermitAll
  public Response getMatchSummaryCases(@Auth AuthUser authUser) {
    List<Summary> summaries = summaryService.describeMatchSummaryCases();
    return Response.ok().entity(summaries).build();
  }


}
