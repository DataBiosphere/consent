package org.broadinstitute.consent.http.health;

import com.codahale.metrics.health.HealthCheck;
import com.google.api.client.http.HttpStatusCodes;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.StringEntity;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
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

public class SamHealthCheckTest {

  @Mock private HttpClientUtil clientUtil;

  @Mock private CloseableHttpResponse response;

  @Mock private StatusLine statusLine;

  @Mock private ServicesConfiguration servicesConfiguration;

  private SamHealthCheck healthCheck;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  private void initHealthCheck() {
    try {
      when(response.getEntity())
          .thenReturn(
              new StringEntity(
                  "{\"ok\":true,\"systems\":{\"GooglePubSub\": {\"ok\": true},\"Database\": {\"ok\": true},\"GoogleGroups\": {\"ok\": true},\"GoogleIam\": {\"ok\": true},\"OpenDJ\": {\"ok\": true}}}"));
      when(clientUtil.getHttpResponse(any())).thenReturn(response);
      when(servicesConfiguration.getSamUrl()).thenReturn("http://localhost:8000/");
      healthCheck = new SamHealthCheck(clientUtil, servicesConfiguration);
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
