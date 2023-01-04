package org.broadinstitute.consent.http.health;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.codahale.metrics.health.HealthCheck;
import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class SendGridHealthCheckTest {
    @Mock
    private HttpClientUtil clientUtil;

    @Mock
    private ClassicHttpResponse response;

    @Mock
    private MailConfiguration mailConfiguration;

    private SendGridHealthCheck healthCheck;
    private SendGridStatus goodStatus, badStatus;

    @Before
    public void setUp() {
        openMocks(this);

        goodStatus = new SendGridStatus();
        goodStatus.setPage("test");
        goodStatus.setStatus(new SendGridStatus.StatusObject(SendGridStatus.Indicator.none, "test"));

        badStatus = new SendGridStatus();
        badStatus.setPage("test");
        badStatus.setStatus(new SendGridStatus.StatusObject(SendGridStatus.Indicator.major, "test"));
    }

    private void initHealthCheck(SendGridStatus status, boolean configOk) {
        try {
            String statusJson = new Gson().toJson(status);
            when(response.getEntity()).thenReturn(new StringEntity(statusJson));
            when(clientUtil.getHttpResponse(any())).thenReturn(response);
            if (configOk) {
                when(mailConfiguration.getSendGridStatusUrl()).thenReturn("http://localhost:8000");
            }
            healthCheck = new SendGridHealthCheck(clientUtil, mailConfiguration);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testCheckSuccess() throws Exception {
        when(response.getCode()).thenReturn(HttpStatusCodes.STATUS_CODE_OK);
        initHealthCheck(goodStatus, true);

        HealthCheck.Result result = healthCheck.check();
        assertTrue(result.isHealthy());
    }

    @Test
    public void testCheckFailure() throws Exception {
        when(response.getCode()).thenReturn(HttpStatusCodes.STATUS_CODE_SERVER_ERROR);
        initHealthCheck(goodStatus, true);

        HealthCheck.Result result = healthCheck.check();
        assertFalse(result.isHealthy());
    }

    @Test
    public void testCheckExternalFailure() throws Exception {
        when(response.getCode()).thenReturn(HttpStatusCodes.STATUS_CODE_OK);
        initHealthCheck(badStatus, true);

        HealthCheck.Result result = healthCheck.check();
        assertFalse(result.isHealthy());
    }

    @Test
    public void testCheckException() throws Exception {
        doThrow(new RuntimeException()).when(response).getCode();
        initHealthCheck(goodStatus, true);

        HealthCheck.Result result = healthCheck.check();
        assertFalse(result.isHealthy());
    }

    @Test
    public void testConfigException() throws Exception {
        doThrow(new RuntimeException()).when(mailConfiguration).getSendGridStatusUrl();
        initHealthCheck(goodStatus, false);

        HealthCheck.Result result = healthCheck.check();
        assertFalse(result.isHealthy());
    }
}