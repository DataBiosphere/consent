package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.broadinstitute.consent.http.models.DarMetricsSummary;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.service.MetricsService;

@Path("api/metrics")
public class MetricsResource extends Resource {

  private final MetricsService metricsService;

  @Inject
  public MetricsResource(MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  @SuppressWarnings("unused")
  @GET
  @Path("/dar-summaries/{datasetId}")
  @Produces("application/json")
  @PermitAll
  public Response getDarSummaryData(
      @Auth DuosUser user, @PathParam("datasetId") Integer datasetId) {
    try {
      List<DarMetricsSummary> summaries = metricsService.generateDarSummaries(datasetId);
      return Response.ok().entity(summaries).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
