package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.DatasetMetrics;
import org.broadinstitute.consent.http.service.MetricsService;

@Path("/metrics")
public class MetricsResource extends Resource {

  private final MetricsService metricsService;

  @Inject
  public MetricsResource(MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  @GET
  @Path("/dataset/{datasetId}")
  @Produces("application/json")
  public Response getDatasetMetricsData(@PathParam("datasetId") Integer datasetId) {
    try {
      DatasetMetrics metrics = metricsService.generateDatasetMetrics(datasetId);
      return Response.ok().entity(metrics).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

}
