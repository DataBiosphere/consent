package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.DarDecisionMetrics;
import org.broadinstitute.consent.http.service.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/metrics")
public class MetricsResource extends Resource {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final MetricsService metricsService;

  @Inject
  public MetricsResource(MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  @GET
  @Path("/dar")
  @Produces(MediaType.TEXT_PLAIN)
  public Response getMetricsData() {
    logger.info("Getting Metrics Data");
    String joiner = "\t";
    StringBuilder tsv = new StringBuilder(DarDecisionMetrics.getHeaderRow(joiner));
    metricsService.generateDarDecisionMetrics().forEach(m -> tsv.append(m.toString(joiner)));
    return Response.ok(tsv.toString()).build();
  }

}
