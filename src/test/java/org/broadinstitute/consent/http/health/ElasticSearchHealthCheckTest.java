package org.broadinstitute.consent.http.health;

import com.codahale.metrics.health.HealthCheck;
import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.apache.http.StatusLine;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.broadinstitute.consent.http.service.ontology.ElasticSearchSupport;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

public class ElasticSearchHealthCheckTest {
    @Mock
    private RestClient client;

    @Mock
    private ElasticSearchConfiguration config;

    @Mock
    private StatusLine statusLine;

    @Mock
    private Response esResponse;

    private ElasticSearchHealthCheck healthCheck;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        List<String> servers = new ArrayList<>();
        servers.add("localhost");
        when(config.getServers()).thenReturn(servers);
        when(ElasticSearchSupport.createRestClient(config)).thenReturn(client);
    }

    private void initHealthCheck(String status) {
        try {
            String stringResponse = IOUtils.toString(esResponse.getEntity().getContent(), Charset.defaultCharset());
            JsonObject jsonResponse = JsonParser.parseString(stringResponse).getAsJsonObject();
            when(jsonResponse.get("status").getAsString()).thenReturn(status);
            when(client.performRequest(any())).thenReturn(esResponse);

            healthCheck = new ElasticSearchHealthCheck(config);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testCheckSuccessGreen() throws Exception {
        when(statusLine.getStatusCode()).thenReturn(HttpStatusCodes.STATUS_CODE_OK);
        when(esResponse.getStatusLine()).thenReturn(statusLine);
        initHealthCheck("green");

        HealthCheck.Result result = healthCheck.check();
        assertTrue(result.isHealthy());
    }

    @Test
    public void testCheckSuccessYellow() throws Exception {
        when(statusLine.getStatusCode()).thenReturn(HttpStatusCodes.STATUS_CODE_OK);
        when(esResponse.getStatusLine()).thenReturn(statusLine);
        initHealthCheck("yellow");

        HealthCheck.Result result = healthCheck.check();
        assertTrue(result.isHealthy());
    }

    @Test
    public void testCheckFailureRed() throws Exception {
        when(statusLine.getStatusCode()).thenReturn(HttpStatusCodes.STATUS_CODE_OK);
        when(esResponse.getStatusLine()).thenReturn(statusLine);
        initHealthCheck("red");

        HealthCheck.Result result = healthCheck.check();
        assertFalse(result.isHealthy());
    }

    @Test
    public void testCheckServerFailure() throws Exception {
        when(statusLine.getStatusCode()).thenReturn(HttpStatusCodes.STATUS_CODE_SERVER_ERROR);
        when(esResponse.getStatusLine()).thenReturn(statusLine);
        initHealthCheck("green");

        HealthCheck.Result result = healthCheck.check();
        assertFalse(result.isHealthy());
    }

    @Test
    public void testCheckException() throws Exception {
        doThrow(new RuntimeException()).when(esResponse).getStatusLine();
        initHealthCheck("green");

        HealthCheck.Result result = healthCheck.check();
        assertFalse(result.isHealthy());
    }
}