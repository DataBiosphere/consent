package org.broadinstitute.consent.http.resources;

import static org.broadinstitute.consent.http.ConsentModule.DB_ENV;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.codahale.metrics.health.HealthCheck.Result;
import com.codahale.metrics.health.HealthCheckRegistry;
import java.util.LinkedHashMap;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.ConsentApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class StatusResourceTest {

    @Mock
    private HealthCheckRegistry healthChecks;

    @BeforeEach
    public void setUp() {
        openMocks(this);
    }

    private StatusResource initStatusResource(SortedMap<String, Result> checks) {
        when(healthChecks.runHealthChecks()).thenReturn(checks);
        return new StatusResource(healthChecks);
    }

    @Test
    public void testHealthy() {
        SortedMap<String, Result> checks = new TreeMap<>();
        checks.put(DB_ENV, Result.healthy());
        checks.put(ConsentApplication.ONTOLOGY_CHECK, Result.healthy());
        StatusResource statusResource = initStatusResource(checks);

        Response response = statusResource.getStatus();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testUnhealthyDatabase() {
        Result postgresql = Result.unhealthy(new Exception("Cannot connect to the postgresql database"));
        SortedMap<String, Result> checks = new TreeMap<>();
        checks.put(DB_ENV, postgresql);
        checks.put(ConsentApplication.ONTOLOGY_CHECK, Result.healthy());
        StatusResource statusResource = initStatusResource(checks);

        Response response = statusResource.getStatus();
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testUnhealthyOntology() {
        Result ontology = Result.unhealthy("Ontology is down");
        SortedMap<String, Result> checks = new TreeMap<>();
        checks.put(DB_ENV, Result.healthy());
        checks.put(ConsentApplication.ONTOLOGY_CHECK, ontology);
        StatusResource statusResource = initStatusResource(checks);

        Response response = statusResource.getStatus();
        // A failing ontology check should not fail the status
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testNotDegraded() {
        SortedMap<String, Result> checks = new TreeMap<>();
        checks.put(DB_ENV, Result.healthy());
        checks.put(ConsentApplication.ONTOLOGY_CHECK, Result.healthy());
        checks.put(ConsentApplication.ES_CHECK, Result.healthy());
        checks.put(ConsentApplication.GCS_CHECK, Result.healthy());
        checks.put(ConsentApplication.SAM_CHECK, Result.healthy());
        checks.put(ConsentApplication.SG_CHECK, Result.healthy());
        StatusResource statusResource = initStatusResource(checks);

        Response response = statusResource.getStatus();
        assertEquals(200, response.getStatus());
        @SuppressWarnings("rawtypes")
        LinkedHashMap content = (LinkedHashMap) response.getEntity();
        assertEquals(Boolean.FALSE, content.get(StatusResource.DEGRADED));
    }

    @Test
    public void testDegraded() {
        SortedMap<String, Result> checks = new TreeMap<>();
        checks.put(DB_ENV, Result.healthy());
        checks.put(ConsentApplication.ONTOLOGY_CHECK, Result.healthy());
        checks.put(ConsentApplication.ES_CHECK, Result.healthy());
        checks.put(ConsentApplication.GCS_CHECK, Result.healthy());
        checks.put(ConsentApplication.SAM_CHECK, Result.unhealthy("Sam is Down"));
        StatusResource statusResource = initStatusResource(checks);

        Response response = statusResource.getStatus();
        // A failing sam check should not fail the status
        assertEquals(200, response.getStatus());
        // A failing sam check should mark the system as degraded
        @SuppressWarnings("rawtypes")
        LinkedHashMap content = (LinkedHashMap) response.getEntity();
        assertEquals(Boolean.TRUE, content.get(StatusResource.DEGRADED));
    }
}
