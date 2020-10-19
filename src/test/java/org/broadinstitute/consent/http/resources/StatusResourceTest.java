package org.broadinstitute.consent.http.resources;

import com.codahale.metrics.health.HealthCheck.Result;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.Response;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.broadinstitute.consent.http.ConsentModule.DB_ENV;
import static org.mockito.Mockito.when;

public class StatusResourceTest {

    private Result postgresql;

    @Mock
    private HealthCheckRegistry healthChecks;

    @Before
    public void setUp() {
        postgresql = Result.healthy();
    }

    private StatusResource initStatusResource(SortedMap<String, Result> checks) {
        MockitoAnnotations.initMocks(this);
        when(healthChecks.runHealthChecks()).thenReturn(checks);
        return new StatusResource(healthChecks);
    }

    @Test
    public void testHealthy() {
        SortedMap<String, Result> checks = new TreeMap<>();
        checks.put(DB_ENV, postgresql);
        StatusResource statusResource = initStatusResource(checks);

        Response response = statusResource.getStatus();
        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void testUnhealthyDatabase() {
        postgresql = Result.unhealthy(new Exception("Cannot connect to the postgresql database"));
        SortedMap<String, Result> checks = new TreeMap<>();
        checks.put(DB_ENV, postgresql);
        StatusResource statusResource = initStatusResource(checks);

        Response response = statusResource.getStatus();
        Assert.assertEquals(500, response.getStatus());
    }

}
