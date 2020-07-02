package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import java.util.List;
import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.DarDecisionMetrics;
import org.broadinstitute.consent.http.service.MetricsService;

@Path("/metrics")
public class MetricsResource extends Resource {

  private final MetricsService metricsService;

  @Inject
  public MetricsResource(MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  @GET
  @Path("/dar")
  @PermitAll
  public Response getMetricsData() {
    List<DarDecisionMetrics> metrics = metricsService.generateDarDecisionMetrics();
    return Response.ok(metrics).build();
  }

}
