package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.models.Type;
import org.broadinstitute.consent.http.service.MetricsService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/metrics")
public class MetricsResource extends Resource {

  private final MetricsService metricsService;
  private static final String JOINER = "\t";

  @Inject
  public MetricsResource(MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  @GET
  @Path("/dar/decision")
  @Produces(MediaType.TEXT_PLAIN)
  public Response getDarMetricsData() {
    return getMetricsData(Type.DAR);
  }

  @GET
  @Path("/dac/decision")
  @Produces(MediaType.TEXT_PLAIN)
  public Response getDacMetricsData() {
    return getMetricsData(Type.DAC);
  }

  private Response getMetricsData(Type type) {
    String header = metricsService.getHeaderRow(type);
    StringBuilder tsv = new StringBuilder(header);
    metricsService.generateDecisionMetrics(type).forEach(m -> tsv.append(m.toString(JOINER)));
    return Response.ok(tsv.toString()).build();
  }
}
