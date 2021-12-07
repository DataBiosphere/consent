package org.broadinstitute.consent.http.health;

import com.codahale.metrics.health.HealthCheck;
import com.google.api.client.http.HttpStatusCodes;
import org.broadinstitute.consent.http.WithMockServer;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.testcontainers.containers.MockServerContainer;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class ElasticSearchHealthCheckTest implements WithMockServer {
    private ElasticSearchHealthCheck healthCheck;
    private ElasticSearchConfiguration config;
    private MockServerClient mockServerClient;

    @Rule
    public MockServerContainer container = new MockServerContainer(IMAGE);

    @Before
    public void setUp() {
        openMocks(this);

        config = new ElasticSearchConfiguration();
        config.setServers(Collections.singletonList("localhost"));
        config.setPort(container.getServerPort());

        mockServerClient = new MockServerClient(container.getHost(), container.getServerPort());
    }

    @After
    public void tearDown() {
        stop(container);
    }

    private void initHealthCheck(String status, Integer statusCode) {
        try {
            String stringResponse = "{ \"status\": \"" + status + "\" }";
            mockServerClient.when(request()).respond(response()
                    .withStatusCode(statusCode)
                    .withBody(stringResponse));

            healthCheck = new ElasticSearchHealthCheck(config);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testCheckSuccessGreen() throws Exception {
        initHealthCheck("green", HttpStatusCodes.STATUS_CODE_OK);

        HealthCheck.Result result = healthCheck.check();
        assertTrue(result.isHealthy());
    }

    @Test
    public void testCheckSuccessYellow() throws Exception {
        initHealthCheck("yellow", HttpStatusCodes.STATUS_CODE_OK);

        HealthCheck.Result result = healthCheck.check();
        assertTrue(result.isHealthy());
    }

    @Test
    public void testCheckFailureRed() throws Exception {
        initHealthCheck("red", HttpStatusCodes.STATUS_CODE_OK);

        HealthCheck.Result result = healthCheck.check();
        assertFalse(result.isHealthy());
    }

    @Test
    public void testCheckServerFailure() throws Exception {
        initHealthCheck("green", HttpStatusCodes.STATUS_CODE_SERVER_ERROR);

        HealthCheck.Result result = healthCheck.check();
        assertFalse(result.isHealthy());
    }
}
