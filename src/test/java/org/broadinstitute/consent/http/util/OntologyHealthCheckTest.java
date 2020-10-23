package org.broadinstitute.consent.http.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.codahale.metrics.health.HealthCheck;
import com.google.api.client.http.HttpStatusCodes;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.service.ontology.OntologyHealthCheck;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class OntologyHealthCheckTest {

  @Mock private HttpClientUtil clientUtil;

  @Mock private CloseableHttpResponse response;

  @Mock private StatusLine statusLine;

  @Mock private ServicesConfiguration servicesConfiguration;

  private OntologyHealthCheck healthCheck;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  private void initHealthCheck() {
    try {
      when(clientUtil.getHttpResponse(any())).thenReturn(response);
      when(servicesConfiguration.getOntologyURL()).thenReturn("http://localhost:8000/");
      healthCheck = new OntologyHealthCheck(clientUtil, servicesConfiguration);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testCheckSuccess() {
    when(statusLine.getStatusCode()).thenReturn(HttpStatusCodes.STATUS_CODE_OK);
    when(response.getStatusLine()).thenReturn(statusLine);
    initHealthCheck();

    HealthCheck.Result result = healthCheck.check();
    assertTrue(result.isHealthy());
  }

  @Test
  public void testCheckFailure() {
    when(statusLine.getStatusCode()).thenReturn(HttpStatusCodes.STATUS_CODE_SERVER_ERROR);
    when(response.getStatusLine()).thenReturn(statusLine);
    initHealthCheck();

    HealthCheck.Result result = healthCheck.check();
    assertFalse(result.isHealthy());
  }

  @Test
  public void testCheckException() {
    doThrow(new RuntimeException()).when(response).getStatusLine();
    initHealthCheck();

    HealthCheck.Result result = healthCheck.check();
    assertFalse(result.isHealthy());
  }
}
