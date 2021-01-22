package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.DacDecisionMetrics;
import org.broadinstitute.consent.http.models.DarDecisionMetrics;
import org.broadinstitute.consent.http.models.DecisionMetrics;
import org.broadinstitute.consent.http.service.MetricsService;

@Path("/metrics")
public class MetricsResource extends Resource {

  private final MetricsService metricsService;

  @Inject
  public MetricsResource(MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  @GET
  @Path("/dar/decision")
  @Produces(MediaType.TEXT_PLAIN)
  public Response getMetricsData() {
    String joiner = "\t";
    String header = DecisionMetrics.getHeaderRow(joiner);
    StringBuilder tsv = new StringBuilder(header);

    metricsService.generateDecisionMetrics().forEach(m -> tsv.append(m.toString(joiner)));
    return Response.ok(tsv.toString()).build();
  }

  @GET
  @Path("/dac/decision")
  @Produces(MediaType.TEXT_PLAIN)
  public Response getDacMetricsData() {
    String joiner = "\t";
    String header = DecisionMetrics.getHeaderRow(joiner);
    StringBuilder tsv = new StringBuilder(header);
    metricsService.generateDacDecisionMetrics().forEach(m -> tsv.append(m.toString(joiner)));
    return Response.ok(tsv.toString()).build();
  }
}
