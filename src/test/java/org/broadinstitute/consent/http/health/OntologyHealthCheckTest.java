package org.broadinstitute.consent.http.health;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.codahale.metrics.health.HealthCheck;
import com.google.api.client.http.HttpStatusCodes;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.broadinstitute.consent.http.util.HttpClientUtil.SimpleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class OntologyHealthCheckTest {

  @Mock
  private HttpClientUtil clientUtil;

  @Mock
  private SimpleResponse response;

  @Mock
  private ServicesConfiguration servicesConfiguration;

  private OntologyHealthCheck healthCheck;

  @BeforeEach
  public void setUp() {
    openMocks(this);
  }

  private void initHealthCheck(boolean configOk) {
    try {
      when(response.entity()).thenReturn("{}");
      when(clientUtil.getCachedResponse(any())).thenReturn(response);
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
    when(response.code()).thenReturn(HttpStatusCodes.STATUS_CODE_OK);
    initHealthCheck(true);

    HealthCheck.Result result = healthCheck.check();
    assertTrue(result.isHealthy());
  }

  @Test
  public void testCheckFailure() {
    when(response.code()).thenReturn(HttpStatusCodes.STATUS_CODE_SERVER_ERROR);
    initHealthCheck(true);

    HealthCheck.Result result = healthCheck.check();
    assertFalse(result.isHealthy());
  }

  @Test
  public void testCheckException() {
    doThrow(new RuntimeException()).when(response).code();
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
