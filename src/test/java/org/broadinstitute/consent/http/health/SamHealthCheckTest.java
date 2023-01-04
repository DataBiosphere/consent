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
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class SamHealthCheckTest {

  @Mock private HttpClientUtil clientUtil;

  @Mock private ClassicHttpResponse response;

  @Mock private ServicesConfiguration servicesConfiguration;

  private SamHealthCheck healthCheck;

  @Before
  public void setUp() {
    openMocks(this);
  }

  private void initHealthCheck(boolean configOk) {
    try {
      when(response.getEntity())
          .thenReturn(
              new StringEntity(
                  "{\"ok\":true,\"systems\":{\"GooglePubSub\": {\"ok\": true},\"Database\": {\"ok\": true},\"GoogleGroups\": {\"ok\": true},\"GoogleIam\": {\"ok\": true},\"OpenDJ\": {\"ok\": true}}}"));
      when(clientUtil.getHttpResponse(any())).thenReturn(response);
      if (configOk) {
        when(servicesConfiguration.getSamUrl()).thenReturn("http://localhost:8000/");
      }
      healthCheck = new SamHealthCheck(clientUtil, servicesConfiguration);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testCheckSuccess() throws Exception {
    when(response.getCode()).thenReturn(HttpStatusCodes.STATUS_CODE_OK);
    initHealthCheck(true);

    HealthCheck.Result result = healthCheck.check();
    assertTrue(result.isHealthy());
  }

  @Test
  public void testCheckFailure() throws Exception {
    when(response.getCode()).thenReturn(HttpStatusCodes.STATUS_CODE_SERVER_ERROR);
    initHealthCheck(true);

    HealthCheck.Result result = healthCheck.check();
    assertFalse(result.isHealthy());
  }

  @Test
  public void testCheckException() throws Exception {
    initHealthCheck(true);

    HealthCheck.Result result = healthCheck.check();
    assertFalse(result.isHealthy());
  }

  @Test
  public void testConfigException() throws Exception {
    doThrow(new RuntimeException()).when(servicesConfiguration).getSamUrl();
    initHealthCheck(false);

    HealthCheck.Result result = healthCheck.check();
    assertFalse(result.isHealthy());
  }
}
