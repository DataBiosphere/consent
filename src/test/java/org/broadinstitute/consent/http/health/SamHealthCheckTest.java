package org.broadinstitute.consent.http.health;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.codahale.metrics.health.HealthCheck;
import com.google.api.client.http.HttpStatusCodes;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.broadinstitute.consent.http.util.HttpClientUtil.SimpleResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class SamHealthCheckTest {

    @Mock
    private HttpClientUtil clientUtil;

    @Mock
    private SimpleResponse response;

    @Mock
    private ServicesConfiguration servicesConfiguration;

    private SamHealthCheck healthCheck;

    @BeforeEach
    public void setUp() {
        openMocks(this);
    }

    private void initHealthCheck(boolean configOk) {
        String okResponse = """
                {
                  "ok": true,
                  "systems": {
                    "GooglePubSub": {"ok": true},
                    "Database": {"ok": true},
                    "GoogleGroups": {"ok": true},
                    "GoogleIam": {"ok": true},
                    "OpenDJ": {"ok": true}
                  }
                }
                """;
        try {
            when(response.entity())
                    .thenReturn(okResponse);
            when(clientUtil.getCachedResponse(any())).thenReturn(response);
            if (configOk) {
                when(servicesConfiguration.getSamUrl()).thenReturn("http://localhost:8000/");
            }
            healthCheck = new SamHealthCheck(clientUtil, servicesConfiguration);
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
    }

    @Test
    public void testCheckSuccess() throws Exception {
        when(response.code()).thenReturn(HttpStatusCodes.STATUS_CODE_OK);
        initHealthCheck(true);

        HealthCheck.Result result = healthCheck.check();
        assertTrue(result.isHealthy());
    }

    @Test
    public void testCheckFailure() throws Exception {
        when(response.code()).thenReturn(HttpStatusCodes.STATUS_CODE_SERVER_ERROR);
        initHealthCheck(true);

        HealthCheck.Result result = healthCheck.check();
        Assertions.assertFalse(result.isHealthy());
    }

    @Test
    public void testCheckException() throws Exception {
        initHealthCheck(true);

        HealthCheck.Result result = healthCheck.check();
        Assertions.assertFalse(result.isHealthy());
    }

    @Test
    public void testConfigException() throws Exception {
        doThrow(new RuntimeException()).when(servicesConfiguration).getSamUrl();
        initHealthCheck(false);

        HealthCheck.Result result = healthCheck.check();
        Assertions.assertFalse(result.isHealthy());
    }
}
