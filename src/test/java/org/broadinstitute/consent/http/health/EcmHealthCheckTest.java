package org.broadinstitute.consent.http.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.health.HealthCheck;
import com.google.api.client.http.HttpStatusCodes;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.broadinstitute.consent.http.util.HttpClientUtil.SimpleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EcmHealthCheckTest {

  @Mock private HttpClientUtil clientUtil;
  @Mock private SimpleResponse response;
  @Mock private ServicesConfiguration servicesConfiguration;

  private EcmHealthCheck healthCheck;

  @BeforeEach
  void setUp() {
    healthCheck = new EcmHealthCheck(clientUtil, servicesConfiguration);
  }

  @Test
  void testCheckSuccess() throws Exception {
    String okResponse =
        """
        {
          "ok": true,
          "systems": {
            "postgres": true
          }
        }
        """;
    when(servicesConfiguration.getEcmStatusUrl()).thenReturn("http://localhost:8000/status");
    when(clientUtil.getCachedResponse(any())).thenReturn(response);
    when(response.code()).thenReturn(HttpStatusCodes.STATUS_CODE_OK);
    when(response.entity()).thenReturn(okResponse);

    HealthCheck.Result result = healthCheck.check();

    assertTrue(result.isHealthy());
    assertEquals(Boolean.TRUE, result.getDetails().get("ok"));
    ArgumentCaptor<HttpGet> request = ArgumentCaptor.forClass(HttpGet.class);
    verify(clientUtil).getCachedResponse(request.capture());
    assertEquals("http://localhost:8000/status", request.getValue().getUri().toString());
  }

  @Test
  void testCheckFailure() throws Exception {
    when(servicesConfiguration.getEcmStatusUrl()).thenReturn("http://localhost:8000/status");
    when(clientUtil.getCachedResponse(any())).thenReturn(response);
    when(response.code()).thenReturn(HttpStatusCodes.STATUS_CODE_SERVICE_UNAVAILABLE);

    HealthCheck.Result result = healthCheck.check();

    assertFalse(result.isHealthy());
  }

  @Test
  void testCheckUnhealthyResponseBody() throws Exception {
    String unhealthyResponse =
        """
        {
          "ok": false,
          "systems": {
            "postgres": false
          }
        }
        """;
    when(servicesConfiguration.getEcmStatusUrl()).thenReturn("http://localhost:8000/status");
    when(clientUtil.getCachedResponse(any())).thenReturn(response);
    when(response.code()).thenReturn(HttpStatusCodes.STATUS_CODE_OK);
    when(response.entity()).thenReturn(unhealthyResponse);

    HealthCheck.Result result = healthCheck.check();

    assertFalse(result.isHealthy());
    assertEquals(Boolean.FALSE, result.getDetails().get("ok"));
  }

  @Test
  void testCheckException() throws Exception {
    when(servicesConfiguration.getEcmStatusUrl()).thenReturn("http://localhost:8000/status");
    doThrow(new RuntimeException("ECM unavailable")).when(clientUtil).getCachedResponse(any());

    HealthCheck.Result result = healthCheck.check();

    assertFalse(result.isHealthy());
  }
}
