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

import static org.mockito.Mockito.when;

public class StatusResourceTest {

    private Result mysql;
    private Result mongodb;

    @Mock
    private HealthCheckRegistry healthChecks;

    @Before
    public void setUp() {
        mysql = Result.healthy();
        mongodb = Result.healthy();
    }

    private StatusResource initStatusResource(SortedMap<String, Result> checks) {
        MockitoAnnotations.initMocks(this);
        when(healthChecks.runHealthChecks()).thenReturn(checks);
        return new StatusResource(healthChecks);
    }

    @Test
    public void testHealthy() {
        SortedMap<String, Result> checks = new TreeMap<>();
        checks.put("mysql", mysql);
        checks.put("mongodb", mongodb);
        StatusResource statusResource = initStatusResource(checks);

        Response response = statusResource.getStatus();
        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void testUnhealthyMysql() {
        mysql = Result.unhealthy(new Exception("Cannot connect to the mysql database"));
        SortedMap<String, Result> checks = new TreeMap<>();
        checks.put("mysql", mysql);
        checks.put("mongodb", mongodb);
        StatusResource statusResource = initStatusResource(checks);

        Response response = statusResource.getStatus();
        Assert.assertEquals(500, response.getStatus());
    }

    @Test
    public void testUnhealthyMongo() {
        mongodb = Result.unhealthy(new Exception("Cannot connect to the mongo server"));
        SortedMap<String, Result> checks = new TreeMap<>();
        checks.put("mysql", mysql);
        checks.put("mongodb", mongodb);
        StatusResource statusResource = initStatusResource(checks);

        Response response = statusResource.getStatus();
        Assert.assertEquals(500, response.getStatus());
    }

}
