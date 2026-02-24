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
import org.broadinstitute.consent.http.models.DatasetMetrics;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.service.MetricsService;

@Path("{api : (api/)?}metrics")
public class MetricsResource extends Resource {

  private final MetricsService metricsService;

  @Inject
  public MetricsResource(MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  /**
   * @deprecated
   * @param datasetId the id of the dataset for which to generate metrics
   * @return Response containing DatasetMetrics for the given datasetId
   */
  @Deprecated(forRemoval = true, since = "2026-02-23")
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
