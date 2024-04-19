package org.broadinstitute.consent.http.health;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.codahale.metrics.health.HealthCheck;
import com.google.api.client.http.HttpStatusCodes;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.broadinstitute.consent.http.util.HttpClientUtil.SimpleResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OntologyHealthCheckTest {

  @Mock
  private HttpClientUtil clientUtil;

  @Mock
  private SimpleResponse response;

  @Mock
  private ServicesConfiguration servicesConfiguration;

  private OntologyHealthCheck healthCheck;

  @Test
  void testCheckSuccess() {
    when(response.code()).thenReturn(HttpStatusCodes.STATUS_CODE_OK);
    try {
      when(response.entity()).thenReturn("{}");
      when(clientUtil.getCachedResponse(any())).thenReturn(response);
      when(servicesConfiguration.getOntologyURL()).thenReturn("http://localhost:8000/");
      healthCheck = new OntologyHealthCheck(clientUtil, servicesConfiguration);
    } catch (Exception e) {
      fail(e.getMessage());
    }
    HealthCheck.Result result = healthCheck.check();
    assertTrue(result.isHealthy());
  }

  @Test
  void testCheckFailure() {
    when(response.code()).thenReturn(HttpStatusCodes.STATUS_CODE_SERVER_ERROR);
    try {
      when(clientUtil.getCachedResponse(any())).thenReturn(response);
      when(servicesConfiguration.getOntologyURL()).thenReturn("http://localhost:8000/");
      healthCheck = new OntologyHealthCheck(clientUtil, servicesConfiguration);
    } catch (Exception e) {
      fail(e.getMessage());
    }

    HealthCheck.Result result = healthCheck.check();
    assertFalse(result.isHealthy());
  }

  @Test
  void testCheckException() {
    doThrow(new RuntimeException()).when(response).code();
    try {
      when(clientUtil.getCachedResponse(any())).thenReturn(response);
      when(servicesConfiguration.getOntologyURL()).thenReturn("http://localhost:8000/");
      healthCheck = new OntologyHealthCheck(clientUtil, servicesConfiguration);
    } catch (Exception e) {
      fail(e.getMessage());
    }

    HealthCheck.Result result = healthCheck.check();
    assertFalse(result.isHealthy());
  }

  @Test
  void testConfigException() {
    doThrow(new RuntimeException()).when(servicesConfiguration).getOntologyURL();
    try {
      healthCheck = new OntologyHealthCheck(clientUtil, servicesConfiguration);
    } catch (Exception e) {
      fail(e.getMessage());
    }

    HealthCheck.Result result = healthCheck.check();
    assertFalse(result.isHealthy());
  }
}
