package org.broadinstitute.consent.http.health;

import com.codahale.metrics.health.HealthCheck;
import com.google.api.client.http.HttpStatusCodes;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.StringEntity;
import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

public class SendGridHealthCheckTest {
    @Mock
    private HttpClientUtil clientUtil;

    @Mock
    private CloseableHttpResponse response;

    @Mock
    private StatusLine statusLine;

    @Mock
    private MailConfiguration mailConfiguration;

    private SendGridHealthCheck healthCheck;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private void initHealthCheck() {
        try {
            when(response.getEntity()).thenReturn(
                    new StringEntity("{\"page\":{\"id\":\"3tgl2vf85cht\",\"name\":\"SendGrid\",\"url\":\"https://status.sendgrid.com\",\"time_zone\":\"America/Los_Angeles\",\"updated_at\":\"2021-11-03T04:01:21.355-07:00\"},\"status\":{\"indicator\":\"none\",\"description\":\"All Systems Operational\"}}")
            );
            when(clientUtil.getHttpResponse(any())).thenReturn(response);
            when(mailConfiguration.getSendGridStatusUrl()).thenReturn("http://localhost:8000");
            healthCheck = new SendGridHealthCheck(clientUtil, mailConfiguration);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testCheckSuccess() throws Exception {
        when(statusLine.getStatusCode()).thenReturn(HttpStatusCodes.STATUS_CODE_OK);
        when(response.getStatusLine()).thenReturn(statusLine);
        initHealthCheck();

        HealthCheck.Result result = healthCheck.check();
        assertTrue(result.isHealthy());
    }

    @Test
    public void testCheckFailure() throws Exception {
        when(statusLine.getStatusCode()).thenReturn(HttpStatusCodes.STATUS_CODE_SERVER_ERROR);
        when(response.getStatusLine()).thenReturn(statusLine);
        initHealthCheck();

        HealthCheck.Result result = healthCheck.check();
        assertFalse(result.isHealthy());
    }

    @Test
    public void testCheckException() throws Exception {
        doThrow(new RuntimeException()).when(response).getStatusLine();
        initHealthCheck();

        HealthCheck.Result result = healthCheck.check();
        assertFalse(result.isHealthy());
    }
}
