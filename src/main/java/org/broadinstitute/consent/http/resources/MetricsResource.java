package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.DatasetMetrics;
import org.broadinstitute.consent.http.models.Type;
import org.broadinstitute.consent.http.service.MetricsService;

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

    private Response getMetricsData(Type type) {
        try {
            String header = metricsService.getHeaderRow(type);
            StringBuilder tsv = new StringBuilder(header);
            metricsService.generateDecisionMetrics(type).forEach(m -> tsv.append(m.toString(JOINER)));
            return Response.ok(tsv.toString()).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }
}
