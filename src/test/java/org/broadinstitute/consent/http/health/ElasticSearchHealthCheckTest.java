package org.broadinstitute.consent.http.health;

import com.codahale.metrics.health.HealthCheck;
import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.apache.http.StatusLine;
import org.broadinstitute.consent.http.WithMockServer;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.broadinstitute.consent.http.service.ontology.ElasticSearchSupport;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockserver.client.MockServerClient;
import org.testcontainers.containers.MockServerContainer;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class ElasticSearchHealthCheckTest implements WithMockServer {
    private ElasticSearchHealthCheck healthCheck;
    private ElasticSearchConfiguration config;
    private RestClient client;
    private MockServerClient mockServerClient;

    @Rule
    public MockServerContainer container = new MockServerContainer(IMAGE);

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        config = new ElasticSearchConfiguration();
        config.setServers(Collections.singletonList("localhost"));
        config.setPort(container.getServerPort());
        client = ElasticSearchSupport.createRestClient(config);

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

    @Test
    public void testCheckException() throws Exception {
//        doThrow(new RuntimeException()).when(esResponse).getStatusLine();
        initHealthCheck("green", HttpStatusCodes.STATUS_CODE_OK);

        HealthCheck.Result result = healthCheck.check();
        assertFalse(result.isHealthy());
    }
}