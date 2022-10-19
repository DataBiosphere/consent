package org.broadinstitute.consent.http.resources;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.broadinstitute.consent.http.ConsentApplication;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.broadinstitute.consent.http.ConsentModule.DB_ENV;

@Path("status")
public class StatusResource extends Resource {

    public static final String OK = "ok";
    public static final String DEGRADED = "degraded";
    public static final String SYSTEMS = "systems";

    private final HealthCheckRegistry healthChecks;

    public StatusResource(HealthCheckRegistry healthChecks) {
        this.healthChecks = healthChecks;
    }

    @GET
    @Produces("application/json")
    public Response getStatus() {
        Map<String, HealthCheck.Result> results = healthChecks.runHealthChecks();
        HealthCheck.Result postgresql = results.getOrDefault(DB_ENV, HealthCheck.Result.unhealthy("Unable to access postgresql database"));
        if (postgresql.isHealthy()) {
            return Response.ok(formatResults(results)).build();
        } else {
            results.entrySet().
                    stream().
                    filter(e -> !e.getValue().isHealthy()).
                    forEach(e -> logWarn("Error in service " + e.getKey() + ": " + formatResultError(e.getValue())));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(formatResults(results)).build();
        }
    }

    private Map<String, Object> formatResults(Map<String, HealthCheck.Result> results) {
        // Order is important so we put Ok and Degraded states at the beginning of the map and
        // add all other entries after that.
        Map<String, Object> formattedResults = new LinkedHashMap<>();
        boolean healthy = false;
        HealthCheck.Result postgresql = results.getOrDefault(DB_ENV, HealthCheck.Result.unhealthy("Unable to access postgresql database"));
        if (postgresql.isHealthy()) {
            healthy = true;
        }
        formattedResults.put(OK, healthy);
        HealthCheck.Result gcs = results.getOrDefault(ConsentApplication.GCS_CHECK, HealthCheck.Result.unhealthy("Unable to access Google Cloud Storage"));
        HealthCheck.Result elasticSearch = results.getOrDefault(ConsentApplication.ES_CHECK, HealthCheck.Result.unhealthy("Unable to access Elastic Search"));
        HealthCheck.Result ontology = results.getOrDefault(ConsentApplication.ONTOLOGY_CHECK, HealthCheck.Result.unhealthy("Unable to access Ontology"));
        HealthCheck.Result sam = results.getOrDefault(ConsentApplication.SAM_CHECK, HealthCheck.Result.unhealthy("Unable to access Sam"));
        HealthCheck.Result sendGrid = results.getOrDefault(ConsentApplication.SG_CHECK, HealthCheck.Result.unhealthy("Unable to access SendGrid"));
        boolean degraded = (!gcs.isHealthy()
                || !elasticSearch.isHealthy()
                || !ontology.isHealthy()
                || !sam.isHealthy()
                || !sendGrid.isHealthy());
        formattedResults.put(DEGRADED, degraded);
        formattedResults.put(SYSTEMS, results);
        return formattedResults;
    }

    private String formatResultError(HealthCheck.Result result) {
        if (result.getMessage() != null) {
            return result.getMessage();
        } else if (result.getError() != null) {
            return result.getError().toString();
        }
        return "Healthcheck Result Error";
    }

}
