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
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.StatusLine;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class OntologyHealthCheckTest {

  @Mock private HttpClientUtil clientUtil;

  @Mock private CloseableHttpResponse response;

  @Mock private StatusLine statusLine;

  @Mock private ServicesConfiguration servicesConfiguration;

  private OntologyHealthCheck healthCheck;

  @Before
  public void setUp() {
    openMocks(this);
  }

  private void initHealthCheck(boolean configOk) {
    try {
      when(response.getEntity()).thenReturn(new StringEntity("{}"));
      when(clientUtil.getHttpResponse(any())).thenReturn(response);
      if (configOk) {
        when(servicesConfiguration.getOntologyURL()).thenReturn("http://localhost:8000/");
      }
      healthCheck = new OntologyHealthCheck(clientUtil, servicesConfiguration);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testCheckSuccess() {
    when(statusLine.getStatusCode()).thenReturn(HttpStatusCodes.STATUS_CODE_OK);
    initHealthCheck(true);

    HealthCheck.Result result = healthCheck.check();
    assertTrue(result.isHealthy());
  }

  @Test
  public void testCheckFailure() {
    when(statusLine.getStatusCode()).thenReturn(HttpStatusCodes.STATUS_CODE_SERVER_ERROR);
    initHealthCheck(true);

    HealthCheck.Result result = healthCheck.check();
    assertFalse(result.isHealthy());
  }

  @Test
  public void testCheckException() {
    doThrow(new RuntimeException()).when(response).getCode();
    initHealthCheck(true);

    HealthCheck.Result result = healthCheck.check();
    assertFalse(result.isHealthy());
  }

  @Test
  public void testConfigException() {
    doThrow(new RuntimeException()).when(servicesConfiguration).getOntologyURL();
    initHealthCheck(false);

    HealthCheck.Result result = healthCheck.check();
    assertFalse(result.isHealthy());
  }
}
